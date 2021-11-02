package eldlreasoning.rules;


import eldlreasoning.expressions.Expression;
import eldlreasoning.expressions.InitExpression;
import eldlreasoning.expressions.LinkExpression;
import eldlreasoning.premises.ELPremiseContext;

import java.util.Queue;

/**
 * init(C) ⇐ E ⊑ ∃R.C
 */
public class DeriveInitFromReachabilityRule extends OWLELRule {


    public DeriveInitFromReachabilityRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        super(premiseContext, toDo);
    }

    @Override
    public void evaluate(Expression expression) {
        if (expression instanceof LinkExpression) {
            evaluate((LinkExpression) expression);
        }
    }

    public void evaluate(LinkExpression subExpr) {
        toDo.add(new InitExpression(subExpr.getSecondConcept()));
    }
}
