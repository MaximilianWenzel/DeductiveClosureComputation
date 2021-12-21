package benchmark.echoclosure;

import data.Closure;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Collection;
import java.util.List;

public class EchoClosure implements Closure<EchoAxiom> {

    private UnifiedSet<EchoAxiom> axioms = new UnifiedSet<>();

    @Override
    public boolean add(EchoAxiom e) {
        return axioms.add(e);
    }

    @Override
    public boolean addAll(List<EchoAxiom> initialAxioms) {
        return axioms.addAll(initialAxioms);
    }

    @Override
    public boolean contains(EchoAxiom e) {
        return axioms.contains(e);
    }

    @Override
    public boolean remove(EchoAxiom o) {
        return axioms.remove(o);
    }

    @Override
    public Collection<EchoAxiom> getClosureResults() {
        return axioms;
    }
}
