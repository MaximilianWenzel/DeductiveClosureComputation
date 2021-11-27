package reasoning.saturation.parallel;

import data.Closure;
import data.ParallelToDo;
import enums.SaturationStatusMessage;
import reasoning.reasoner.IncrementalReasonerImpl;
import reasoning.rules.InferenceProcessor;
import reasoning.rules.Rule;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class SaturationPartition implements Runnable {

    private static final AtomicLong partitionIDCounter = new AtomicLong(1L);

    private final long id = partitionIDCounter.getAndIncrement();
    private final Collection<? extends Rule> rules;
    private final Closure closure;
    private final ParallelToDo toDo;
    private final ParallelSaturation controlNode;
    private final IncrementalReasonerImpl incrementalReasoner;

    private boolean saturationConverged = false;

    public SaturationPartition(ParallelSaturation controlNode, Collection<? extends Rule> rules,
                               Closure closure,
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
        this.incrementalReasoner = new IncrementalReasonerImpl(rules, closure);
    }

    @Override
    public void run() {
        try {
            while (!controlNode.allPartitionsConverged()) {
                if (toDo.isEmpty()) {
                    sendStatusToControlNode(SaturationStatusMessage.PARTITION_INFO_TODO_IS_EMPTY);
                    saturationConverged = true;
                }

                Object axiom = toDo.take();

                if (saturationConverged) {
                    saturationConverged = false;
                    sendStatusToControlNode(SaturationStatusMessage.PARTITION_INFO_SATURATION_RUNNING);
                }

                incrementalReasoner.processAxiom(axiom);
            }

        } catch (InterruptedException e) {
            // thread terminated
        }
    }

    public Set<Object> getClosure() {
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
        SaturationPartition that = (SaturationPartition) o;
        return id == that.id;
    }

    public void sendStatusToControlNode(SaturationStatusMessage statusMessage) {
        controlNode.getStatusMessages().add(statusMessage);
    }
}
