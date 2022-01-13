package benchmark.eldlreasoning.rules;

import eldlsyntax.*;
import reasoning.rules.InferenceProcessor;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * C ⊑ ⊤ ⇐ no preconditions
 */
public class SubsumedByTopRule extends OWLELRule {

    private ELConcept topConcept;

    private SubsumedByTopRuleVisitor visitor;

    public SubsumedByTopRule(ELConcept topConcept) {
        super();
        this.topConcept = topConcept;
        visitor = new SubsumedByTopRuleVisitor(topConcept);
    }

    public SubsumedByTopRule() {
        visitor = new SubsumedByTopRuleVisitor(topConcept);
    }

    @Override
    public Stream<ELConceptInclusion> streamOfInferences(ELConceptInclusion axiom) {
        Stream.Builder<ELConceptInclusion> inferences = Stream.builder();
        visitor.setStreamBuilder(inferences);
        axiom.getSubConcept().accept(visitor);
        axiom.getSuperConcept().accept(visitor);
        return visitor.getInferenceStream();
    }

}
