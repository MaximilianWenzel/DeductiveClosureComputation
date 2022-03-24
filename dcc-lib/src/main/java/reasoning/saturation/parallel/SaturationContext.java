package reasoning.saturation.parallel;

import data.Closure;
import enums.SaturationStatusMessage;
import enums.StatisticsComponent;
import networking.messages.AxiomCount;
import networking.messages.RequestAxiomMessageCount;
import reasoning.reasoner.DefaultIncrementalReasoner;
import reasoning.rules.MultithreadedSaturationConclusionProcessor;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a worker task which is deployed in the multi-threaded deductive closure computation procedure.
 */
public class SaturationContext<C extends Closure<A>, A extends Serializable>
        implements Runnable {

    private static final AtomicLong workerIDCounter = new AtomicLong(1L);

    private final long id = workerIDCounter.getAndIncrement();
    private final Collection<? extends Rule<C, A>> rules;
    private final C closure;
    private final BlockingQueue<Object> toDo;
    private final MultithreadedSaturation<C, A> controlNode;

    private final AtomicInteger receivedAxioms = new AtomicInteger(0);
    private final AtomicInteger sentAxioms = new AtomicInteger(0);
    private final AtomicInteger saturationStage = new AtomicInteger(0);

    private final SaturationConfiguration config;
    private DefaultIncrementalReasoner<C, A> incrementalReasoner;
    private final WorkloadDistributor<C, A> workloadDistributor;
    private MultithreadedSaturationConclusionProcessor<C, A> conclusionProcessor;

    private boolean lastMessageWasAxiomCountRequest = false;
    private WorkerStatistics statistics = null;

    public SaturationContext(SaturationConfiguration config, MultithreadedSaturation<C, A> controlNode,
                             Collection<? extends Rule<C, A>> rules,
                             WorkloadDistributor<C, A> workloadDistributor,
                             C closure,
                             BlockingQueue<Object> toDo) {
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
        this.incrementalReasoner = new DefaultIncrementalReasoner<>(rules, closure, config, statistics);
    }

    @Override
    public void run() {
        try {
            while (!controlNode.allWorkersConverged()) {
                Object message;
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
                    message = toDo.poll();
                }

                if (message == SaturationStatusMessage.CONTROL_NODE_REQUEST_SEND_CLOSURE_RESULT) {
                    // stop this loop
                    break;
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
                    Collection<A> conclusions = incrementalReasoner.getConclusionsForGivenAxiom((A) message);

                    for (A conclusion : conclusions) {
                        this.conclusionProcessor.processConclusion(conclusion);
                    }
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
        SaturationContext<C, A> that = (SaturationContext<C, A>) o;
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

    public void setConclusionProcessor(MultithreadedSaturationConclusionProcessor<C, A> conclusionProcessor) {
        this.conclusionProcessor = conclusionProcessor;
        this.rules.forEach(r -> {
            r.setClosure(closure);
        });
    }

    public BlockingQueue<Object> getToDo() {
        return toDo;
    }

    public AtomicInteger getSentAxioms() {
        return sentAxioms;
    }

    public WorkerStatistics getStatistics() {
        return statistics;
    }
}
