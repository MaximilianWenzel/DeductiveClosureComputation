package benchmark.eldlreasoning.rules;

import eldlsyntax.*;

import java.io.Serializable;
import java.util.stream.Stream;

public class SubsumedByTopRuleVisitor implements ELConcept.Visitor, Serializable {
    Stream.Builder<ELConceptInclusion> streamBuilder;
    private ELConcept topConcept;

    SubsumedByTopRuleVisitor() {

    }

    public SubsumedByTopRuleVisitor(ELConcept topConcept) {
        this.topConcept = topConcept;
    }

    public void setStreamBuilder(Stream.Builder<ELConceptInclusion> streamBuilder) {
        this.streamBuilder = streamBuilder;
    }

    public Stream<ELConceptInclusion> getConclusionStream() {
        return streamBuilder.build();
    }

    @Override
    public void visit(ELConceptBottom concept) {
        streamBuilder.add(new ELConceptInclusion(concept, topConcept));
    }

    @Override
    public void visit(ELConceptConjunction concept) {
        streamBuilder.add(new ELConceptInclusion(concept, topConcept));
        concept.getFirstConjunct().accept(this);
        concept.getSecondConjunct().accept(this);
    }

    @Override
    public void visit(ELConceptExistentialRestriction concept) {
        streamBuilder.add(new ELConceptInclusion(concept, topConcept));
        concept.getFiller().accept(this);
    }

    @Override
    public void visit(ELConceptName concept) {
        streamBuilder.add(new ELConceptInclusion(concept, topConcept));
    }

    @Override
    public void visit(ELConceptTop concept) {
        streamBuilder.add(new ELConceptInclusion(concept, topConcept));
    }


}
