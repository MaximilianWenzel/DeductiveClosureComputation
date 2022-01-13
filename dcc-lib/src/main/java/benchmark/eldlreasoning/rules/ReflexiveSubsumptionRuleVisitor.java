package benchmark.eldlreasoning.rules;

import eldlsyntax.*;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ReflexiveSubsumptionRuleVisitor implements ELConcept.Visitor, Serializable {
    private Stream.Builder<ELConceptInclusion> inferenceBuilder;

    public ReflexiveSubsumptionRuleVisitor() {

    }

    public Stream.Builder<ELConceptInclusion> getInferenceBuilder() {
        return inferenceBuilder;
    }

    public void setInferenceBuilder(Stream.Builder<ELConceptInclusion> inferenceBuilder) {
        this.inferenceBuilder = inferenceBuilder;
    }

    @Override
    public void visit(ELConceptBottom concept) {
        inferenceBuilder.add(new ELConceptInclusion(concept, concept));
    }

    @Override
    public void visit(ELConceptConjunction concept) {
        inferenceBuilder.add(new ELConceptInclusion(concept, concept));
        concept.getFirstConjunct().accept(this);
        concept.getSecondConjunct().accept(this);
    }

    @Override
    public void visit(ELConceptExistentialRestriction concept) {
        inferenceBuilder.add(new ELConceptInclusion(concept, concept));
        concept.getFiller().accept(this);
    }

    @Override
    public void visit(ELConceptName concept) {
        inferenceBuilder.add(new ELConceptInclusion(concept, concept));
    }

    @Override
    public void visit(ELConceptTop concept) {
        inferenceBuilder.add(new ELConceptInclusion(concept, concept));
    }
}
