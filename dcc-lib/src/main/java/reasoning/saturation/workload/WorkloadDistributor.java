package reasoning.saturation.workload;

import data.Closure;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * A class that can be used in order to obtain all responsible workers for a given axiom.
 * @param <C> Type of the resulting deductive closure.
 * @param <A> Type of the axioms in the deductive closure.
 */
public abstract class WorkloadDistributor<C extends Closure<A>, A extends Serializable> implements Serializable {



    public WorkloadDistributor() {
    }

    public abstract Stream<Long> getRelevantWorkerIDsForAxiom(A axiom);
}
