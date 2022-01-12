package benchmark.eldlreasoning.rules;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptExistentialRestriction;
import eldlsyntax.ELConceptInclusion;

/**
 * E ⊑ ∃R.D ⇐ E ⊑ ∃R.C ∧ C ⊑ D
 */
public class UnfoldExistentialRule extends OWLELRule {

    public UnfoldExistentialRule() {
        super();
    }

    @Override
    public void apply(ELConceptInclusion axiom) {
        if (!(axiom.getSuperConcept() instanceof ELConceptExistentialRestriction)) {
            return;
        }

        ELConcept e = axiom.getSubConcept();
        ELConceptExistentialRestriction exist = (ELConceptExistentialRestriction) axiom.getSuperConcept();
        ELConcept c = exist.getFiller();

        for (Object obj : closure) {
            if (!(obj instanceof ELConceptInclusion)) {
                continue;
            }
            ELConceptInclusion conceptInclusion = (ELConceptInclusion) obj;
            if (c.equals(conceptInclusion.getSubConcept())) {
                ELConcept d = conceptInclusion.getSuperConcept();
                ELConceptExistentialRestriction existForSupertype = new ELConceptExistentialRestriction(exist.getRelation(), d);
                processInference(new ELConceptInclusion(e, existForSupertype));
            }
        }
    }
}
