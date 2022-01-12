package benchmark.eldlreasoning.rules;

import eldlsyntax.*;

import java.io.Serializable;
import java.util.function.Consumer;

public class ReflexiveSubsumptionRuleVisitor implements ELConcept.Visitor, Serializable {
    private Consumer<ELConceptInclusion> consumer;

    ReflexiveSubsumptionRuleVisitor() {

    }

    public ReflexiveSubsumptionRuleVisitor(Consumer<ELConceptInclusion> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void visit(ELConceptBottom concept) {
        consumer.accept(new ELConceptInclusion(concept, concept));
    }

    @Override
    public void visit(ELConceptConjunction concept) {
        consumer.accept(new ELConceptInclusion(concept, concept));
        concept.getFirstConjunct().accept(this);
        concept.getSecondConjunct().accept(this);
    }

    @Override
    public void visit(ELConceptExistentialRestriction concept) {
        consumer.accept(new ELConceptInclusion(concept, concept));
        concept.getFiller().accept(this);
    }

    @Override
    public void visit(ELConceptName concept) {
        consumer.accept(new ELConceptInclusion(concept, concept));
    }

    @Override
    public void visit(ELConceptTop concept) {
        consumer.accept(new ELConceptInclusion(concept, concept));
    }
}
