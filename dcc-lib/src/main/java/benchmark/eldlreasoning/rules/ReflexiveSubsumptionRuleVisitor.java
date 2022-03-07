package benchmark.eldlreasoning.rules;

import eldlsyntax.*;

import java.io.Serializable;
import java.util.stream.Stream;

public class ReflexiveSubsumptionRuleVisitor implements ELConcept.Visitor, Serializable {
    private Stream.Builder<ELConceptInclusion> conclusionBuilder;

    public ReflexiveSubsumptionRuleVisitor() {

    }

    public Stream.Builder<ELConceptInclusion> getConclusionBuilder() {
        return conclusionBuilder;
    }

    public void setConclusionBuilder(Stream.Builder<ELConceptInclusion> conclusionBuilder) {
        this.conclusionBuilder = conclusionBuilder;
    }

    @Override
    public void visit(ELConceptBottom concept) {
        conclusionBuilder.add(new ELConceptInclusion(concept, concept));
    }

    @Override
    public void visit(ELConceptConjunction concept) {
        conclusionBuilder.add(new ELConceptInclusion(concept, concept));
        concept.getFirstConjunct().accept(this);
        concept.getSecondConjunct().accept(this);
    }

    @Override
    public void visit(ELConceptExistentialRestriction concept) {
        conclusionBuilder.add(new ELConceptInclusion(concept, concept));
        concept.getFiller().accept(this);
    }

    @Override
    public void visit(ELConceptName concept) {
        conclusionBuilder.add(new ELConceptInclusion(concept, concept));
    }

    @Override
    public void visit(ELConceptTop concept) {
        conclusionBuilder.add(new ELConceptInclusion(concept, concept));
    }
}
