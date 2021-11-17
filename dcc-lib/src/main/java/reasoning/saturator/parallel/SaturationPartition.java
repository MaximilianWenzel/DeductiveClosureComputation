package reasoning.saturator.parallel;

import data.Closure;
import data.Dataset;
import data.ParallelToDo;
import reasoning.rules.Rule;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class SaturationPartition<P, T> implements Runnable {

    private static final AtomicLong partitionIDCounter = new AtomicLong(1L);

    private final long id = partitionIDCounter.getAndIncrement();
    private final Set<T> termPartition;
    private final Collection<? extends Rule<P>> rules;
    private final Dataset<P, T> datasetFragment;
    private final Closure<P> closure;
    private final ParallelToDo<P> toDo;
    private final ParallelSaturation<P, T> mainNode;

    public SaturationPartition(Collection<? extends Rule<P>> rules,
                               Set<T> termPartition,
                               Dataset<P, T> datasetFragment,
                               Closure<P> closure,
                               ParallelToDo<P> toDo,
                               ParallelSaturation<P, T> mainNode) {
        this.termPartition = termPartition;
        this.datasetFragment = datasetFragment;
        this.closure = closure;
        this.toDo = toDo;
        this.mainNode = mainNode;
        this.rules = rules;
        this.rules.forEach(r -> {
            r.setToDo(mainNode.getToDo());
            r.setClosure(closure);
        });
    }

    @Override
    public void run() {
        try {
            while (!mainNode.isSaturationFinished()) {
                P axiom = toDo.take();
                process(axiom);
            }

        } catch (InterruptedException e) {
            // thread terminated
        }
    }

    private void process(P axiom) {
        if (mainNode.checkIfOtherPartitionsRequireAxiom(this, axiom)) {
            mainNode.getToDo().add(axiom);
        }
        if (mainNode.isRelevantAxiomToPartition(this, axiom)) {
            if (closure.add(axiom)) {
                for (Rule<P> rule : rules) {
                    rule.apply(axiom);
                }
            }
        }
    }

    public BlockingQueue<P> getToDo() {
        return toDo;
    }

    public Set<P> getClosure() {
        return closure;
    }

    public Set<T> getTermPartition() {
        return termPartition;
    }

    public Dataset<P, T> getDatasetFragment() {
        return datasetFragment;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaturationPartition<?, ?> that = (SaturationPartition<?, ?>) o;
        return id == that.id;
    }
}
