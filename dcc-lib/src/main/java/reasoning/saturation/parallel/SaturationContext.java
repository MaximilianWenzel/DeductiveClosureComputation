package reasoning.saturation.parallel;

import data.Closure;
import data.ParallelToDo;
import networking.messages.AxiomCount;
import networking.messages.RequestAxiomMessageCount;
import reasoning.reasoner.IncrementalReasonerImpl;
import reasoning.rules.ParallelSaturationInferenceProcessor;
import reasoning.rules.Rule;

import java.io.Serializable;
import java.util.Collection;
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
    private final IncrementalReasonerImpl<C, A> incrementalReasoner;

    private AtomicInteger receivedAxioms = new AtomicInteger(0);
    private AtomicInteger sentAxioms = new AtomicInteger(0);
    private AtomicInteger saturationStage = new AtomicInteger(0);
    private boolean lastMessageWasAxiomCountRequest = false;

    public SaturationContext(ParallelSaturation<C, A, T> controlNode, Collection<? extends Rule<C, A>> rules,
                             C closure,
                             ParallelToDo toDo) {
        this.controlNode = controlNode;
        this.closure = closure;
        this.toDo = toDo;
        this.rules = rules;

        this.incrementalReasoner = new IncrementalReasonerImpl<>(rules, closure);
    }

    @Override
    public void run() {
        try {
            while (!controlNode.allWorkersConverged()) {
                if (toDo.isEmpty()) {
                    if (!this.lastMessageWasAxiomCountRequest) {
                        sendAxiomCountToControlNode();
                    }
                }
                Serializable message = toDo.take();

                if (message instanceof RequestAxiomMessageCount) {
                    RequestAxiomMessageCount axiomCountRequest = (RequestAxiomMessageCount) message;
                    this.saturationStage.set(axiomCountRequest.getStage());
                    sendAxiomCountToControlNode();
                    this.lastMessageWasAxiomCountRequest = true;
                } else {
                    if (this.lastMessageWasAxiomCountRequest) {
                        this.lastMessageWasAxiomCountRequest = false;
                    }
                    receivedAxioms.incrementAndGet();
                    incrementalReasoner.processAxiom((A) message);
                }
            }

        } catch (InterruptedException e) {
            // thread terminated
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
        controlNode.getStatusMessages()
                .add(new AxiomCount(this.id,
                        this.saturationStage.get(),
                        this.sentAxioms.getAndSet(0),
                        this.receivedAxioms.getAndSet(0)));
    }

    public void setInferenceProcessor(ParallelSaturationInferenceProcessor<C, A, T> inferenceProcessor) {
        this.rules.forEach(r -> {
            r.setInferenceProcessor(inferenceProcessor);
            r.setClosure(closure);
        });
    }

    public ParallelToDo getToDo() {
        return toDo;
    }

    public AtomicInteger getSentAxioms() {
        return sentAxioms;
    }
}
