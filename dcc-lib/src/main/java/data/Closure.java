package data;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface Closure<A extends Serializable> {

    /**
     * Returns whether the inserted element is new.
     */
    boolean add(A axiom);
    boolean addAll(List<A> axioms);
    boolean contains(A axiom);

    boolean remove(A axiom);

    Collection<A> getClosureResults();
}
