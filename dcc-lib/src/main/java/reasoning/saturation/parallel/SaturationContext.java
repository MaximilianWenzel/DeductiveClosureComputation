package reasoning.saturation.parallel;

import data.Closure;
import data.ParallelToDo;
import enums.SaturationStatusMessage;
import reasoning.reasoner.IncrementalReasonerImpl;
import reasoning.rules.InferenceProcessor;
import reasoning.rules.Rule;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class SaturationContext<C extends Closure<A>, A extends Serializable> implements Runnable {

    private static final AtomicLong partitionIDCounter = new AtomicLong(1L);

    private final long id = partitionIDCounter.getAndIncrement();
    private final Collection<? extends Rule<C, A>> rules;
    private final C closure;
    private final ParallelToDo<A> toDo;
    private final ParallelSaturation<C, A> controlNode;
    private final IncrementalReasonerImpl<C, A> incrementalReasoner;

    private boolean saturationConverged = false;

    public SaturationContext(ParallelSaturation<C, A> controlNode, Collection<? extends Rule<C, A>> rules,
                             C closure,
                             ParallelToDo toDo,
                             InferenceProcessor inferenceProcessor) {
        this.controlNode = controlNode;
        this.closure = closure;
        this.toDo = toDo;
        this.rules = rules;
        this.rules.forEach(r -> {
            r.setInferenceProcessor(inferenceProcessor);
            r.setClosure(closure);
        });
        this.incrementalReasoner = new IncrementalReasonerImpl<>(rules, closure);
    }

    @Override
    public void run() {
        try {
            while (!controlNode.allPartitionsConverged()) {
                if (toDo.isEmpty()) {
                    sendStatusToControlNode(SaturationStatusMessage.WORKER_INFO_SATURATION_CONVERGED);
                    saturationConverged = true;
                }

                A axiom = toDo.take();

                if (saturationConverged) {
                    saturationConverged = false;
                    sendStatusToControlNode(SaturationStatusMessage.WORKER_INFO_SATURATION_RUNNING);
                }

                incrementalReasoner.processAxiom(axiom);
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
        SaturationContext that = (SaturationContext) o;
        return id == that.id;
    }

    public void sendStatusToControlNode(SaturationStatusMessage statusMessage) {
        controlNode.getStatusMessages().add(statusMessage);
    }
}
