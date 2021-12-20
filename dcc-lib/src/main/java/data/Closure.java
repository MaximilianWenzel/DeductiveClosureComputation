package data;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface Closure<A extends Serializable> {

    boolean add(A e);
    boolean addAll(List<A> initialAxioms);
    boolean contains(A e);

    boolean remove(A o);

    Collection<A> getClosureResults();
}
