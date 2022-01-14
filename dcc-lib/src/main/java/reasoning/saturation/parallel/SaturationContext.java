package reasoning.saturation.parallel;

import data.Closure;
import data.ParallelToDo;
import data.ToDoQueue;
import enums.StatisticsComponent;
import networking.messages.AxiomCount;
import networking.messages.RequestAxiomMessageCount;
import reasoning.reasoner.IncrementalStreamReasoner;
import reasoning.rules.ParallelSaturationInferenceProcessor;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SaturationContext<C extends Closure<A>, A extends Serializable, T extends Serializable>
        implements Runnable {

    private static final AtomicLong workerIDCounter = new AtomicLong(1L);

    private final long id = workerIDCounter.getAndIncrement();
    private final Collection<? extends Rule<C, A>> rules;
    private final C closure;
    private final ParallelToDo toDo;
    private final ParallelSaturation<C, A, T> controlNode;

    private final AtomicInteger receivedAxioms = new AtomicInteger(0);
    private final AtomicInteger sentAxioms = new AtomicInteger(0);
    private final AtomicInteger saturationStage = new AtomicInteger(0);

    private final SaturationConfiguration config;
    private IncrementalStreamReasoner<C, A> incrementalReasoner;
    private WorkloadDistributor<C, A, T> workloadDistributor;
    private ParallelSaturationInferenceProcessor<C, A, T> inferenceProcessor;

    private boolean lastMessageWasAxiomCountRequest = false;
    private WorkerStatistics statistics = null;

    public SaturationContext(SaturationConfiguration config, ParallelSaturation<C, A, T> controlNode,
                             Collection<? extends Rule<C, A>> rules,
                             WorkloadDistributor<C, A, T> workloadDistributor,
                             C closure,
                             ParallelToDo toDo) {
        this.config = config;
        this.controlNode = controlNode;
        this.workloadDistributor = workloadDistributor;
        this.closure = closure;
        this.toDo = toDo;
        this.rules = rules;

        init();
    }

    private void init() {
        if (config.collectWorkerNodeStatistics()) {
            this.statistics = new WorkerStatistics();
        }
        this.incrementalReasoner = new IncrementalStreamReasoner<>(rules, closure, config, statistics);
    }

    @Override
    public void run() {
        try {
            while (!controlNode.allWorkersConverged()) {
                Serializable message;
                if (toDo.isEmpty()) {
                    if (!this.lastMessageWasAxiomCountRequest) {
                        sendAxiomCountToControlNode();
                    }

                    if (config.collectWorkerNodeStatistics()) {
                        this.statistics.startStopwatch(StatisticsComponent.WORKER_WAITING_TIME_SATURATION);
                        this.statistics.getTodoIsEmptyEvent().incrementAndGet();
                    }
                    message = toDo.take();
                    if (config.collectWorkerNodeStatistics()) {
                        this.statistics.stopStopwatch(StatisticsComponent.WORKER_WAITING_TIME_SATURATION);
                    }
                } else {
                    message = toDo.take();
                }

                if (message instanceof RequestAxiomMessageCount) {
                    RequestAxiomMessageCount axiomCountRequest = (RequestAxiomMessageCount) message;
                    this.saturationStage.set(axiomCountRequest.getStage());
                    sendAxiomCountToControlNode();
                    this.lastMessageWasAxiomCountRequest = true;
                } else {
                    if (this.lastMessageWasAxiomCountRequest) {
                        this.lastMessageWasAxiomCountRequest = false;
                    }

                    if (config.collectWorkerNodeStatistics()) {
                        this.statistics.getNumberOfReceivedAxioms().incrementAndGet();
                    }
                    receivedAxioms.incrementAndGet();
                    incrementalReasoner.getStreamOfInferencesForGivenAxiom((A)message)
                            .forEach(inference -> this.inferenceProcessor.processInference(inference));
                }
            }

        } catch (InterruptedException e) {
            // thread terminated
        } finally {
            if (statistics != null) {
                statistics.collectStopwatchTimes();
            }
        }
    }

    public C getClosure() {
        return closure;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaturationContext<C, A, T> that = (SaturationContext<C, A, T>) o;
        return id == that.id;
    }

    public void sendAxiomCountToControlNode() {
        try {
            controlNode.getStatusMessages().put(new AxiomCount(this.id,
                    this.saturationStage.get(),
                    this.sentAxioms.getAndSet(0),
                    this.receivedAxioms.getAndSet(0)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setInferenceProcessor(ParallelSaturationInferenceProcessor<C, A, T> inferenceProcessor) {
        this.inferenceProcessor = inferenceProcessor;
        this.rules.forEach(r -> {
            r.setClosure(closure);
        });
    }

    public ParallelToDo getToDo() {
        return toDo;
    }

    public AtomicInteger getSentAxioms() {
        return sentAxioms;
    }

    public WorkerStatistics getStatistics() {
        return statistics;
    }
}
