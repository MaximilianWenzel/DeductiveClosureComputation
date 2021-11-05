package eldlsyntax;

import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class IndexedELOntology extends ELOntology {

    private final Map<ELConcept, AtomicInteger> conceptNegativeOccurrences = new HashMap<>();
    private final Map<ELConcept, AtomicInteger> conceptPositiveOccurrences = new HashMap<>();
    private final Set<ELConcept> negativeConcepts = new UnifiedSet<>();
    private final ELConcept top = new ELConceptTop();
    private final ELConcept bottom = new ELConceptBottom();


    private AtomicInteger zero = new AtomicInteger(0);


    public IndexedELOntology() {
        super();
    }

    /**
     * Adds the given {@link ELTBoxAxiom} to this ontology
     *
     * @param axiom
     */
    public void add(ELTBoxAxiom axiom) {
        tBox_.add((ELConceptInclusion) axiom);
        axiom.accept(new ELTBoxAxiom.Visitor() {
            @Override
            public void visit(ELConceptInclusion axiom) {
                ELConcept c = axiom.getSubConcept();
                ELConcept d = axiom.getSuperConcept();

                AtomicInteger negativeCounter = conceptNegativeOccurrences.computeIfAbsent(c, e -> new AtomicInteger(0));
                if (negativeCounter.incrementAndGet() > 0) {
                    negativeConcepts.add(c);
                }

                AtomicInteger positiveCounter = conceptPositiveOccurrences.computeIfAbsent(c, e -> new AtomicInteger(0));
                positiveCounter.incrementAndGet();
            }
        });
    }

    /**
     * Adds the given {@link ELABoxAxiom} to this ontology
     *
     * @param axiom
     */
    public void add(ELABoxAxiom axiom) {
        aBox_.add(axiom);
    }


    /**
     * Removes the given {@link ELTBoxAxiom} from this ontology
     *
     * @param axiom
     */
    public void remove(ELTBoxAxiom axiom) {
        axiom.accept(new ELTBoxAxiom.Visitor() {
            @Override
            public void visit(ELConceptInclusion axiom) {
                tBox_.remove(axiom);
                ELConcept c = axiom.getSubConcept();
                ELConcept d = axiom.getSuperConcept();

                AtomicInteger negativeCounter = conceptNegativeOccurrences.get(c);
                negativeCounter.decrementAndGet();

                AtomicInteger positiveCounter = conceptPositiveOccurrences.computeIfAbsent(d, e -> new AtomicInteger(0));
                positiveCounter.decrementAndGet();

                updateLookupStructures(c);
                updateLookupStructures(d);
            }
        });
    }

    public void updateLookupStructures(ELConcept concept) {
        int negativeOccurrences = conceptNegativeOccurrences.getOrDefault(concept, zero).get();
        if (negativeOccurrences == 0) {
            negativeConcepts.remove(concept);
        }
    }

    /**
     * Removes the given {@link ELABoxAxiom} from this ontology
     *
     * @param axiom
     */
    public void remove(ELABoxAxiom axiom) {
        aBox_.remove(axiom);
    }

    /**
     * Adds the given {@link ELAxiom} to this ontology
     *
     * @param axiom
     * @return the resulting ontology with the added axiom
     */
    public ELOntology add(ELAxiom axiom) {
        axiom = Objects.requireNonNull(axiom);
        axiom.accept(new ELAxiom.Visitor() {

            @Override
            public void visit(ELConceptInclusion axiom) {
                add(axiom);
            }

            @Override
            public void visit(ELRoleAssertion axiom) {
                add(axiom);
            }

            @Override
            public void visit(ELConceptAssertion axiom) {
                add(axiom);
            }
        });
        return this;
    }

    /**
     * Removes the given {@link ELAxiom} from this ontology
     *
     * @param axiom
     * @return the resulting ontology after removal of the axiom
     */
    public ELOntology remove(ELAxiom axiom) {
        axiom = Objects.requireNonNull(axiom);
        axiom.accept(new ELAxiom.Visitor() {

            @Override
            public void visit(ELConceptInclusion axiom) {
                remove(axiom);
            }

            @Override
            public void visit(ELRoleAssertion axiom) {
                remove(axiom);
            }

            @Override
            public void visit(ELConceptAssertion axiom) {
                remove(axiom);
            }
        });
        return this;
    }

    public Set<ELConcept> getAllUsedConceptsInOntology() {
        // TODO inefficient implementation, however lookup structures are otherwise not correct after removal operation
        Set<ELConcept> concepts = new UnifiedSet<>();

        this.tBox_.forEach(conceptIncl -> {
            conceptIncl.getSubConcept().streamOfThisConceptAndAllContainedConcepts()
                    .forEach(concepts::add);
            conceptIncl.getSuperConcept().streamOfThisConceptAndAllContainedConcepts()
                    .forEach(concepts::add);
        });
        concepts.add(top);
        concepts.add(bottom);
        return concepts;
    }


    public Map<ELConcept, AtomicInteger> getConceptNegativeOccurrences() {
        return conceptNegativeOccurrences;
    }

    public Map<ELConcept, AtomicInteger> getConceptPositiveOccurrences() {
        return conceptPositiveOccurrences;
    }

    public Set<ELConcept> getNegativeConcepts() {
        return negativeConcepts;
    }

    public ELConcept getTop() {
        return top;
    }

    public ELConcept getBottom() {
        return bottom;
    }
}
