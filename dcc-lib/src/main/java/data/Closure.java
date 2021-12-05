package data;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface Closure<A extends Serializable> {

    boolean add(A e);
    boolean addAll(List<A> initialAxioms);

    boolean remove(A o);

    Iterable<A> getClosureResults();
}
