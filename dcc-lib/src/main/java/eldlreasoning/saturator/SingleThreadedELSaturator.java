package eldlreasoning.saturator;

import eldlreasoning.rules.*;
import eldlsyntax.ELConceptInclusion;
import eldlsyntax.ELTBoxAxiom;
import eldlsyntax.IndexedELOntology;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.Set;

public class SingleThreadedELSaturator {

    private Set<ELTBoxAxiom> axioms = new UnifiedSet<>();
    private Set<ELConceptInclusion> closure = new UnifiedSet<>();
    private Queue<ELConceptInclusion> toDo;
    private IndexedELOntology elOntology;
    private Collection<Rule> rules;

    public SingleThreadedELSaturator(IndexedELOntology elOntology) {
        this.elOntology = elOntology;
        this.toDo = new ArrayDeque<>();
        this.elOntology.tBox().forEach(elAxiom -> {
            if (elAxiom instanceof ELConceptInclusion) {
                toDo.add((ELConceptInclusion) elAxiom);
            }
        });
        init();
    }

    private void init() {
        // initialize all required rules
        rules = new UnifiedSet<>();
        rules.add(new ComposeConjunctionRule(toDo, elOntology.getNegativeConcepts(), closure));
        rules.add(new DecomposeConjunctionRule(toDo));
        rules.add(new ReflexiveSubsumptionRule(toDo));
        rules.add(new SubsumedByTopRule(toDo, elOntology.getTop()));
        rules.add(new UnfoldExistentialRule(toDo, closure));
        rules.add(new UnfoldSubsumptionRule(toDo, elOntology.getOntologyAxioms()));
    }

    public Set<ELConceptInclusion> saturate() {
        while (!toDo.isEmpty()) {
            process(toDo.remove());
        }
        return closure;
    }

    private void process(ELConceptInclusion axiom) {
        if (closure.add(axiom)) {
            for (Rule rule : rules) {
                // TODO implement more efficient rule application which considers expression type
                rule.apply(axiom);
            }
        }
    }
}
