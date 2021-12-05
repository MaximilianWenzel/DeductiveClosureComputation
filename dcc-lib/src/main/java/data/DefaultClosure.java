package data;

import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.io.Serializable;
import java.util.List;

public class DefaultClosure<A extends Serializable> extends UnifiedSet<A> implements Closure<A> {
    @Override
    public boolean addAll(List<A> initialAxioms) {
        int sizeBefore = this.size();
        initialAxioms.forEach(this::add);
        return sizeBefore != this.size();
    }

    @Override
    public boolean remove(A o) {
        return super.remove(o);
    }

    @Override
    public Iterable<A> getClosureResults() {
        return this;
    }
}
