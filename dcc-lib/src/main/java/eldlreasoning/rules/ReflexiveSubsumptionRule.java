package eldlreasoning.rules;

import eldlreasoning.expression.Expression;
import eldlreasoning.expression.InitExpression;
import eldlreasoning.expression.SubsumptionExpression;
import eldlreasoning.premise.ELPremiseContext;

import java.util.Queue;

/**
 * C ⊑ C ⇐ init(C)
 */
public class ReflexiveSubsumptionRule implements Rule {

    private Queue<Expression> toDo;

    public ReflexiveSubsumptionRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        this.toDo = toDo;
    }

    public void evaluate(Expression expression) {
        if (expression instanceof InitExpression) {
            evaluate((InitExpression) expression);
        }
    }

    public void evaluate(InitExpression initExpression) {
        toDo.add(new SubsumptionExpression(initExpression.getConcept(), initExpression.getConcept()));
    }
}
