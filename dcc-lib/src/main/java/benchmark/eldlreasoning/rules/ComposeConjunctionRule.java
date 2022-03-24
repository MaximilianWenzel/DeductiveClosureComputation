package benchmark.eldlreasoning.rules;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptConjunction;
import eldlsyntax.ELConceptInclusion;

import java.util.Set;
import java.util.stream.Stream;

/**
 * derived(C ⊑ D1 ⊓ D2) ⇐ derived(C ⊑ D1) ∧ derived(C ⊑ D2) ∧ occursNegatively(D1 ⊓ D2)
 *
 * occursNegatively(C) ⇐ told(C ⊑ E)
 * occursNegatively(D1), occursNegatively(D2) ⇐ occursNegatively(D1 ⊓ D2)
 * occursNegatively(C) ⇐ occursNegatively(∃R.C)
 *
 * option 0
 * derived(C ⊑ D1 ⊓ D2) ⇐ derived(C ⊑ D1) ∧ derived(C ⊑ D2) ∧ occursNegatively(D1 ⊓ D2)
 *
 * option 1
 * derivedCandidate(C ⊑ D1 ⊓ D2) ⇐ derived(C ⊑ D1) ∧ derived(C ⊑ D2)
 * derived(C ⊑ E) ⇐ derivedCandidate(C ⊑ E), occursNegatively(E)
 *
 * option 2
 * check(C, D1, D2) ⇐ derived(C ⊑ D2), occursNegatively(D1 ⊓ D2)
 * derived(C ⊑ D1 ⊓ D2) ⇐ derived(C ⊑ D1) ∧ check(C, D1, D2)
 *
 * option 3
 * check(C, D1, n) ⇐ derived(C ⊑ D1) ∧ signatureOfSuperConcepts(C, n)
 * possiblyDerived(C ⊑ D1 ⊓ D2) ⇐ check(C, D1, n), occursNegatively(D1 ⊓ D2) : D2 possibly in n
 * derived(C ⊑ D1 ⊓ D2) ⇐ derived(C ⊑ D2), possiblyDerived(C ⊑ D1 ⊓ D2)
 *
 * signatureOfSuperConcepts(C, n + E) ⇐ derived(C ⊑ E), signatureOfSuperConcepts(C, n)
 *
 *
 */
public class ComposeConjunctionRule extends OWLELRule {

    private Set<ELConcept> negativeConceptsFromOntology;

    public ComposeConjunctionRule() {
    }

    public ComposeConjunctionRule(Set<ELConcept> negativeConceptsFromOntology) {
        super();
        this.negativeConceptsFromOntology = negativeConceptsFromOntology;
    }

    @Override
    public Stream<ELConceptInclusion> streamOfConclusions(ELConceptInclusion axiom) {
        Stream.Builder<ELConceptInclusion> conclusions = Stream.builder();
        ELConcept c = axiom.getSubConcept();
        ELConcept d1 = axiom.getSuperConcept();

        for (ELConceptInclusion conceptInclusion : closure) {
            if (c.equals(conceptInclusion.getSubConcept())) {
                ELConcept d2 = conceptInclusion.getSuperConcept();
                ELConceptConjunction conjunction = new ELConceptConjunction(d1, d2);
                if (this.negativeConceptsFromOntology.contains(conjunction)) {
                    conclusions.add(new ELConceptInclusion(c, conjunction));
                }
                ELConceptConjunction conjunction2 = new ELConceptConjunction(d2, d1);
                if (this.negativeConceptsFromOntology.contains(conjunction2)) {
                    conclusions.add(new ELConceptInclusion(c, conjunction2));
                }
            }
        }
        return conclusions.build();
    }
}
