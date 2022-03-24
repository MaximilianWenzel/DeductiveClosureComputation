package data;

import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class DefaultClosure<A extends Serializable> extends UnifiedSet<A> implements Closure<A> {
    @Override
    public boolean addAll(List<A> axioms) {
        int sizeBefore = this.size();
        axioms.forEach(this::add);
        return sizeBefore != this.size();
    }

    @Override
    public boolean contains(A axiom) {
        return super.contains(axiom);
    }

    @Override
    public boolean remove(A axiom) {
        return super.remove(axiom);
    }

    @Override
    public Collection<A> getClosureResults() {
        return this;
    }
}
