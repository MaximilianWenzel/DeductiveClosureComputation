package data;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * This class is used in the deductive closure computation to store all initial axioms and deduced conclusions that can be derived from the
 * initial axioms and given rules. At the end of the closure computation procedure, this object should correspond to the deductive closure
 * of the given initial axioms and rules.
 *
 * @param <A> Axioms which are to be added to the closure.
 */
public interface Closure<A extends Serializable> {

    /**
     * Returns whether the axiom has not already been added to the closure.
     */
    boolean add(A axiom);

    boolean addAll(List<A> axioms);

    boolean contains(A axiom);

    boolean remove(A axiom);

    Collection<A> getClosureResults();
}
