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
    public Stream<ELConceptInclusion> streamOfInferences(ELConceptInclusion axiom) {
        Stream.Builder<ELConceptInclusion> inferences = Stream.builder();
        if (axiom.getSuperConcept() instanceof ELConceptConjunction) {
            ELConcept c = axiom.getSubConcept();
            ELConceptConjunction conjunction = (ELConceptConjunction) axiom.getSuperConcept();
            ELConcept d1 = conjunction.getFirstConjunct();
            ELConcept d2 = conjunction.getSecondConjunct();
            inferences.add(new ELConceptInclusion(c, d1));
            inferences.add(new ELConceptInclusion(c, d2));
        }
        return inferences.build();
    }
}
