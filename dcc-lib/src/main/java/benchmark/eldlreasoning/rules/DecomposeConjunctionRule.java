package benchmark.eldlreasoning.rules;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptConjunction;
import eldlsyntax.ELConceptInclusion;

import java.util.stream.Stream;

/**
 * C ⊑ D1 ∧ C ⊑ D2 ⇐ C ⊑ D1 ⊓ D2.
 */
public class DecomposeConjunctionRule extends OWLELRule {


    public DecomposeConjunctionRule() {
        super();
    }

    @Override
    public Stream<ELConceptInclusion> streamOfConclusions(ELConceptInclusion axiom) {
        Stream.Builder<ELConceptInclusion> conclusions = Stream.builder();
        if (axiom.getSuperConcept() instanceof ELConceptConjunction) {
            ELConcept c = axiom.getSubConcept();
            ELConceptConjunction conjunction = (ELConceptConjunction) axiom.getSuperConcept();
            ELConcept d1 = conjunction.getFirstConjunct();
            ELConcept d2 = conjunction.getSecondConjunct();
            conclusions.add(new ELConceptInclusion(c, d1));
            conclusions.add(new ELConceptInclusion(c, d2));
        }
        return conclusions.build();
    }
}
