package benchmark.eldlreasoning.rules;

import eldlsyntax.*;
import reasoning.rules.InferenceProcessor;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * C ⊑ C ⇐ no premises
 */
public class ReflexiveSubsumptionRule extends OWLELRule {

    private ReflexiveSubsumptionRuleVisitor visitor;

    public ReflexiveSubsumptionRule() {
        super();
        visitor = new ReflexiveSubsumptionRuleVisitor();
    }

    @Override
    public Stream<ELConceptInclusion> streamOfInferences(ELConceptInclusion axiom) {
        Stream.Builder<ELConceptInclusion> inferences = Stream.builder();
        visitor.setInferenceBuilder(inferences);
        axiom.getSubConcept().accept(visitor);
        axiom.getSuperConcept().accept(visitor);
        return inferences.build();
    }
}
