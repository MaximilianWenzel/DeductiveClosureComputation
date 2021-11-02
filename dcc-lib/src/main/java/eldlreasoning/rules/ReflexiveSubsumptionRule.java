package eldlreasoning.rules;

import eldlreasoning.expressions.Expression;
import eldlreasoning.expressions.InitExpression;
import eldlreasoning.expressions.SubsumptionExpression;
import eldlreasoning.premises.ELPremiseContext;

import java.util.Queue;

/**
 * C ⊑ C ⇐ init(C)
 */
public class ReflexiveSubsumptionRule extends OWLELRule {


    public ReflexiveSubsumptionRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        super(premiseContext, toDo);
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
