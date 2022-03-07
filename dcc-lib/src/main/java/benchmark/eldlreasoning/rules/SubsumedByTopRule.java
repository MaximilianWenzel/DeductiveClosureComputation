package benchmark.eldlreasoning.rules;

import eldlsyntax.*;

import java.util.stream.Stream;

/**
 * C ⊑ ⊤ ⇐ no preconditions
 */
public class SubsumedByTopRule extends OWLELRule {

    private ELConcept topConcept;

    private final SubsumedByTopRuleVisitor visitor;

    public SubsumedByTopRule(ELConcept topConcept) {
        super();
        this.topConcept = topConcept;
        visitor = new SubsumedByTopRuleVisitor(topConcept);
    }

    public SubsumedByTopRule() {
        visitor = new SubsumedByTopRuleVisitor(topConcept);
    }

    @Override
    public Stream<ELConceptInclusion> streamOfConclusions(ELConceptInclusion axiom) {
        Stream.Builder<ELConceptInclusion> conclusions = Stream.builder();
        visitor.setStreamBuilder(conclusions);
        axiom.getSubConcept().accept(visitor);
        axiom.getSuperConcept().accept(visitor);
        return visitor.getConclusionStream();
    }

}
