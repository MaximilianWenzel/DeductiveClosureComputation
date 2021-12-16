package reasoning.saturation.parallel;

import data.Closure;
import data.ParallelToDo;
import enums.SaturationStatusMessage;
import reasoning.reasoner.IncrementalReasonerImpl;
import reasoning.rules.InferenceProcessor;
import reasoning.rules.ParallelSaturationInferenceProcessor;
import reasoning.rules.Rule;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class SaturationContext<C extends Closure<A>, A extends Serializable, T extends Serializable> implements Runnable {

    private static final AtomicLong workerIDCounter = new AtomicLong(1L);

    private final long id = workerIDCounter.getAndIncrement();
    private final Collection<? extends Rule<C, A>> rules;
    private final C closure;
    private final ParallelToDo<A> toDo;
    private final ParallelSaturation<C, A ,T> controlNode;
    private final IncrementalReasonerImpl<C, A> incrementalReasoner;

    private boolean saturationConverged = false;

    public SaturationContext(ParallelSaturation<C, A, T> controlNode, Collection<? extends Rule<C, A>> rules,
                             C closure,
                             ParallelToDo<A> toDo) {
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
                synchronized (toDo) {
                    if (toDo.isEmpty()) {
                        sendStatusToControlNode(SaturationStatusMessage.WORKER_INFO_SATURATION_CONVERGED);
                        saturationConverged = true;
                    }
                }


                A axiom = toDo.take();
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
        SaturationContext<C, A, T> that = (SaturationContext<C, A, T>) o;
        return id == that.id;
    }

    public void sendStatusToControlNode(SaturationStatusMessage statusMessage) {
        controlNode.getStatusMessages().add(statusMessage);
    }

    public void setInferenceProcessor(ParallelSaturationInferenceProcessor<C, A, T> inferenceProcessor) {
        this.rules.forEach(r -> {
            r.setInferenceProcessor(inferenceProcessor);
            r.setClosure(closure);
        });
    }

    public ParallelToDo<A> getToDo() {
        return toDo;
    }

    public boolean isSaturationConverged() {
        return saturationConverged;
    }

    public void setSaturationConverged(boolean saturationConverged) {
        this.saturationConverged = saturationConverged;
    }
}