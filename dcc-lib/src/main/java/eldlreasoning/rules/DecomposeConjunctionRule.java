package eldlreasoning.rules;

import eldlreasoning.expression.Expression;
import eldlreasoning.expression.SubsumptionExpression;
import eldlreasoning.models.IdxConjunction;
import eldlreasoning.premise.ELPremiseContext;

import java.util.Queue;

/**
 * C ⊑ D1 ∧ C ⊑ D2 ⇐ C ⊑ D1 ⊓ D2.
 */
public class DecomposeConjunctionRule implements Rule {

    private final Queue<Expression> toDo;
    private final ELPremiseContext premiseContext;

    public DecomposeConjunctionRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        this.premiseContext = premiseContext;
        this.toDo = toDo;
    }

    @Override
    public void evaluate(Expression expression) {
        if (expression instanceof SubsumptionExpression) {
            evaluate((SubsumptionExpression) expression);
        }
    }

    public void evaluate(SubsumptionExpression subExpr) {
        if (subExpr.getSecondConcept() instanceof IdxConjunction) {
            IdxConjunction conj = (IdxConjunction) subExpr.getSecondConcept();
            toDo.add(new SubsumptionExpression(subExpr.getFirstConcept(), conj.getFirstConcept()));
            toDo.add(new SubsumptionExpression(subExpr.getFirstConcept(), conj.getSecondConcept()));
        }
    }
}
