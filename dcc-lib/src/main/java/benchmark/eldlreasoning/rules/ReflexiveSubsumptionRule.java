package benchmark.eldlreasoning.rules;

import eldlsyntax.*;
import reasoning.rules.InferenceProcessor;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * C ⊑ C ⇐ no premises
 */
public class ReflexiveSubsumptionRule extends OWLELRule {

    private ReflexiveSubsumptionRuleVisitor visitor;

    public ReflexiveSubsumptionRule() {
        super();
    }

    @Override
    public void apply(ELConceptInclusion axiom) {
        axiom.getSubConcept().accept(visitor);
        axiom.getSuperConcept().accept(visitor);
    }

    public void setInferenceProcessor(InferenceProcessor<ELConceptInclusion> inferenceProcessor) {
        this.inferenceProcessor = inferenceProcessor;
        this.visitor = new ReflexiveSubsumptionRuleVisitor(new ProcessInferenceConsumer<>(inferenceProcessor));
    }
}
