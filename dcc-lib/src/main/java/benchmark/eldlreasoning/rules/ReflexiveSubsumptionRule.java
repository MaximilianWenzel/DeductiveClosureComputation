package benchmark.eldlreasoning.rules;

import eldlsyntax.*;

import java.util.stream.Stream;

/**
 * C ⊑ C ⇐ no premises
 */
public class ReflexiveSubsumptionRule extends OWLELRule {

    private final ReflexiveSubsumptionRuleVisitor visitor;

    public ReflexiveSubsumptionRule() {
        super();
        visitor = new ReflexiveSubsumptionRuleVisitor();
    }

    @Override
    public Stream<ELConceptInclusion> streamOfConclusions(ELConceptInclusion axiom) {
        Stream.Builder<ELConceptInclusion> conclusions = Stream.builder();
        visitor.setConclusionBuilder(conclusions);
        axiom.getSubConcept().accept(visitor);
        axiom.getSuperConcept().accept(visitor);
        return conclusions.build();
    }
}
