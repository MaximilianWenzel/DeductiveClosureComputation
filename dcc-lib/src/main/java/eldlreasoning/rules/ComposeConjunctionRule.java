package eldlreasoning.rules;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptConjunction;
import eldlsyntax.ELConceptInclusion;

import java.util.Set;

/**
 * C ⊑ D1 ⊓ D2 ⇐ C ⊑ D1 ∧ C ⊑ D2 : D1 ⊓ D2 occurs negatively in ontology O.
 */
public class ComposeConjunctionRule extends OWLELRule {

    private final Set<ELConcept> negativeConceptsFromOntology;

    public ComposeConjunctionRule(Set<ELConcept> negativeConceptsFromOntology) {
        super();
        this.negativeConceptsFromOntology = negativeConceptsFromOntology;
    }

    @Override
    public void apply(ELConceptInclusion axiom) {

        ELConcept c = axiom.getSubConcept();
        ELConcept d1 = axiom.getSuperConcept();

        for (ELConceptInclusion conceptInclusion : closure) {
            if (c.equals(conceptInclusion.getSubConcept())) {
                ELConcept d2 = conceptInclusion.getSuperConcept();
                ELConceptConjunction conjunction = new ELConceptConjunction(d1, d2);
                if (this.negativeConceptsFromOntology.contains(conjunction)) {
                    this.addToToDo(new ELConceptInclusion(c, conjunction));
                }
                ELConceptConjunction conjunction2 = new ELConceptConjunction(d2, d1);
                if (this.negativeConceptsFromOntology.contains(conjunction2)) {
                    this.addToToDo(new ELConceptInclusion(c, conjunction2));
                }
            }
        }
    }
}
