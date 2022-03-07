package benchmark.echoclosure;

import data.Closure;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Collection;
import java.util.List;

public class EchoClosure implements Closure<EchoAxiom> {

    private final UnifiedSet<EchoAxiom> axioms = new UnifiedSet<>();

    @Override
    public boolean add(EchoAxiom axiom) {
        return axioms.add(axiom);
    }

    @Override
    public boolean addAll(List<EchoAxiom> axioms) {
        return this.axioms.addAll(axioms);
    }

    @Override
    public boolean contains(EchoAxiom axiom) {
        return axioms.contains(axiom);
    }

    @Override
    public boolean remove(EchoAxiom axiom) {
        return axioms.remove(axiom);
    }

    @Override
    public Collection<EchoAxiom> getClosureResults() {
        return axioms;
    }
}
