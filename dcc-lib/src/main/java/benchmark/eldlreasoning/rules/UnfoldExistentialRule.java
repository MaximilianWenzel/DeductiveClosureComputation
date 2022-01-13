package benchmark.eldlreasoning.rules;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptExistentialRestriction;
import eldlsyntax.ELConceptInclusion;

import java.util.stream.Stream;

/**
 * E ⊑ ∃R.D ⇐ E ⊑ ∃R.C ∧ C ⊑ D
 */
public class UnfoldExistentialRule extends OWLELRule {

    public UnfoldExistentialRule() {
        super();
    }

    @Override
    public Stream<ELConceptInclusion> streamOfInferences(ELConceptInclusion axiom) {
        Stream.Builder<ELConceptInclusion> inferences = Stream.builder();
        if (!(axiom.getSuperConcept() instanceof ELConceptExistentialRestriction)) {
            return Stream.empty();
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
                inferences.add(new ELConceptInclusion(e, existForSupertype));
            }
        }
        return inferences.build();
    }
}
