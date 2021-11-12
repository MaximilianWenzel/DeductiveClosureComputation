package eldlreasoning.rules;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptConjunction;
import eldlsyntax.ELConceptInclusion;

/**
 * C ⊑ D1 ∧ C ⊑ D2 ⇐ C ⊑ D1 ⊓ D2.
 */
public class DecomposeConjunctionRule extends OWLELRule {


    public DecomposeConjunctionRule() {
    }

    @Override
    public void apply(ELConceptInclusion axiom) {
        if (axiom.getSuperConcept() instanceof ELConceptConjunction) {
            ELConcept c = axiom.getSubConcept();
            ELConceptConjunction conjunction = (ELConceptConjunction) axiom.getSuperConcept();
            ELConcept d1 = conjunction.getFirstConjunct();
            ELConcept d2 = conjunction.getSecondConjunct();
            addToToDo(new ELConceptInclusion(c, d1));
            addToToDo(new ELConceptInclusion(c, d2));
        }
    }
}
