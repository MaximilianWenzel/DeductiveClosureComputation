package benchmark.eldlreasoning.rules;

import eldlsyntax.*;
import reasoning.rules.InferenceProcessor;

import java.util.function.Consumer;

/**
 * C ⊑ ⊤ ⇐ no preconditions
 */
public class SubsumedByTopRule extends OWLELRule {

    private ELConcept topConcept;

    private SubsumedByTopRuleVisitor visitor;

    public SubsumedByTopRule(ELConcept topConcept) {
        super();
        this.topConcept = topConcept;
    }

    public SubsumedByTopRule() {
    }

    @Override
    public void apply(ELConceptInclusion axiom) {
        axiom.getSubConcept().accept(visitor);
        axiom.getSuperConcept().accept(visitor);
    }

    public void setInferenceProcessor(InferenceProcessor<ELConceptInclusion> inferenceProcessor) {
        this.inferenceProcessor = inferenceProcessor;
        this.visitor = new SubsumedByTopRuleVisitor(new ProcessInferenceConsumer<>(inferenceProcessor), topConcept);
    }
}
