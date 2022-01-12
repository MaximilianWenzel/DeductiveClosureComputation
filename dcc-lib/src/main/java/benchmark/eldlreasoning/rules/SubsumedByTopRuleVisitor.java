package benchmark.eldlreasoning.rules;

import eldlsyntax.*;
import reasoning.rules.InferenceProcessor;

import java.io.Serializable;
import java.util.function.Consumer;

public class SubsumedByTopRuleVisitor implements ELConcept.Visitor, Serializable {
    Consumer<ELConceptInclusion> consumer;
    private ELConcept topConcept;

    SubsumedByTopRuleVisitor() {

    }

    public SubsumedByTopRuleVisitor(Consumer<ELConceptInclusion> consumer, ELConcept topConcept) {
        this.consumer = consumer;
        this.topConcept = topConcept;
    }

    @Override
    public void visit(ELConceptBottom concept) {
        consumer.accept(new ELConceptInclusion(concept, topConcept));
    }

    @Override
    public void visit(ELConceptConjunction concept) {
        consumer.accept(new ELConceptInclusion(concept, topConcept));
        concept.getFirstConjunct().accept(this);
        concept.getSecondConjunct().accept(this);
    }

    @Override
    public void visit(ELConceptExistentialRestriction concept) {
        consumer.accept(new ELConceptInclusion(concept, topConcept));
        concept.getFiller().accept(this);
    }

    @Override
    public void visit(ELConceptName concept) {
        consumer.accept(new ELConceptInclusion(concept, topConcept));
    }

    @Override
    public void visit(ELConceptTop concept) {
        consumer.accept(new ELConceptInclusion(concept, topConcept));
    }


}
