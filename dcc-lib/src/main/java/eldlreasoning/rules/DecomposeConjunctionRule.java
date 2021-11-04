package eldlreasoning.rules;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptConjunction;
import eldlsyntax.ELConceptInclusion;

import java.util.Queue;

/**
 * C ⊑ D1 ∧ C ⊑ D2 ⇐ C ⊑ D1 ⊓ D2.
 */
public class DecomposeConjunctionRule extends OWLELRule {


    public DecomposeConjunctionRule(Queue<ELConceptInclusion> toDo) {
        super(toDo);
    }

    @Override
    public void apply(ELConceptInclusion axiom) {
        if (axiom.getSuperConcept() instanceof ELConceptConjunction) {
            ELConcept c = axiom.getSubConcept();
            ELConceptConjunction conjunction = (ELConceptConjunction) axiom.getSuperConcept();
            ELConcept d1 = conjunction.getFirstConjunct();
            ELConcept d2 = conjunction.getSecondConjunct();
            toDo.add(new ELConceptInclusion(c, d1));
            toDo.add(new ELConceptInclusion(c, d2));
        }
    }
}
