package eldlreasoning.expressions;

public interface Expression extends ConceptStreamBuilder {

    interface Visitor {
        void visit(LinkExpression linkExpression);

        void visit(InitExpression initExpression);

        void visit(SubsumptionExpression subsumptionExpression);
    }


}
