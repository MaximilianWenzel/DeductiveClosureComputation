package eldlreasoning.rules;


import eldlreasoning.expression.Expression;
import eldlreasoning.expression.InitExpression;
import eldlreasoning.expression.LinkExpression;
import eldlreasoning.expression.SubsumptionExpression;
import eldlreasoning.models.IdxConjunction;
import eldlreasoning.premise.ELPremiseContext;

import java.util.Queue;

/**
 * init(C) ⇐ E ⊑ ∃R.C
 */
public class DeriveInitFromReachabilityRule implements Rule {

    private final Queue<Expression> toDo;
    private final ELPremiseContext premiseContext;

    public DeriveInitFromReachabilityRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        this.premiseContext = premiseContext;
        this.toDo = toDo;
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
