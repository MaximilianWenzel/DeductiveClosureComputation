package eldlreasoning.rules;

import eldlreasoning.expression.Expression;
import eldlreasoning.expression.InitExpression;
import eldlreasoning.expression.SubsumptionExpression;
import eldlreasoning.models.IdxConcept;
import eldlreasoning.premise.ELPremiseContext;

import java.util.Queue;

/**
 * C ⊑ ⊤ ⇐ init(C) : ⊤ occurs negatively in ontology O.
 */
public class SubsumedByTopRule implements Rule {

    private final IdxConcept top;

    private final Queue<Expression> toDo;

    public SubsumedByTopRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        this.top = premiseContext.getTop();
        this.toDo = toDo;
    }

    @Override
    public void evaluate(Expression expression) {
        if (expression instanceof InitExpression) {
            evaluate((InitExpression) expression);
        }
    }

    public void evaluate(InitExpression initExpression) {
        if (top.getNegOccurs().get() > 0) {
            toDo.add(new SubsumptionExpression(initExpression.getConcept(), top));
        }
    }
}
