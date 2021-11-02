package eldlreasoning.rules;

import eldlreasoning.expressions.Expression;
import eldlreasoning.expressions.SubsumptionExpression;
import eldlreasoning.models.IdxConjunction;
import eldlreasoning.premises.ELPremiseContext;

import java.util.Queue;

/**
 * C ⊑ D1 ∧ C ⊑ D2 ⇐ C ⊑ D1 ⊓ D2.
 */
public class DecomposeConjunctionRule extends OWLELRule {


    public DecomposeConjunctionRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        super(premiseContext, toDo);
    }

    @Override
    public void evaluate(Expression expression) {
        if (expression instanceof SubsumptionExpression) {
            evaluate((SubsumptionExpression) expression);
        }
    }

    public void evaluate(SubsumptionExpression subExpr) {
        if (subExpr.getSuperConcept() instanceof IdxConjunction) {
            IdxConjunction conj = (IdxConjunction) subExpr.getSuperConcept();
            toDo.add(new SubsumptionExpression(subExpr.getSubConcept(), conj.getFirstConjunct()));
            toDo.add(new SubsumptionExpression(subExpr.getSubConcept(), conj.getSecondConjunct()));
        }
    }
}
