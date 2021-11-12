package eldlreasoning.rules;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptExistentialRestriction;
import eldlsyntax.ELConceptInclusion;

/**
 * E ⊑ ∃R.D ⇐ E ⊑ ∃R.C ∧ C ⊑ D
 */
public class UnfoldExistentialRule extends OWLELRule {

    public UnfoldExistentialRule() {
    }

    @Override
    public void apply(ELConceptInclusion axiom) {
        if (!(axiom.getSuperConcept() instanceof ELConceptExistentialRestriction)) {
            return;
        }

        ELConcept e = axiom.getSubConcept();
        ELConceptExistentialRestriction exist = (ELConceptExistentialRestriction) axiom.getSuperConcept();
        ELConcept c = exist.getFiller();

        for (ELConceptInclusion conceptInclusion : closure) {
            if (c.equals(conceptInclusion.getSubConcept())) {
                ELConcept d = conceptInclusion.getSuperConcept();
                ELConceptExistentialRestriction existForSupertype = new ELConceptExistentialRestriction(exist.getRelation(), d);
                addToToDo(new ELConceptInclusion(e, existForSupertype));
            }
        }
    }
}
