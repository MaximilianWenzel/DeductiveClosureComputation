package eldlreasoning.rules;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptExistentialRestriction;
import eldlsyntax.ELConceptInclusion;

import java.util.Queue;
import java.util.Set;

/**
 * E ⊑ ∃R.D ⇐ E ⊑ ∃R.C ∧ C ⊑ D
 */
public class UnfoldExistentialRule extends OWLELRule {

    private Set<ELConceptInclusion> closure;

    public UnfoldExistentialRule(Queue<ELConceptInclusion> toDo, Set<ELConceptInclusion> closure) {
        super(toDo);
        this.closure = closure;
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
                toDo.add(new ELConceptInclusion(e, existForSupertype));
            }
        }
    }
}
