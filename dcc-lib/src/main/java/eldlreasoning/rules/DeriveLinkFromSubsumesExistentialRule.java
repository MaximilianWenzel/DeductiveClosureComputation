package eldlreasoning.rules;

import eldlreasoning.expressions.Expression;
import eldlreasoning.expressions.LinkExpression;
import eldlreasoning.expressions.SubsumptionExpression;
import eldlreasoning.models.IdxAtomicConcept;
import eldlreasoning.models.IdxExistential;
import eldlreasoning.premises.ELPremiseContext;

import java.util.Queue;

/**
 * E →ᴿ C ⇐ E ⊑ ∃R.C
 */
public class DeriveLinkFromSubsumesExistentialRule extends OWLELRule {
    public DeriveLinkFromSubsumesExistentialRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        super(premiseContext, toDo);
    }

    @Override
    public void evaluate(Expression expression) {
        if (expression instanceof SubsumptionExpression) {
            evaluate((SubsumptionExpression) expression);
        }
    }

    private void evaluate(SubsumptionExpression subsumptionExpression) {
        if (subsumptionExpression.getSubConcept() instanceof IdxAtomicConcept
            && subsumptionExpression.getSuperConcept() instanceof IdxExistential) {
            IdxExistential idxExistential = (IdxExistential) subsumptionExpression.getSuperConcept();
            toDo.add(new LinkExpression(
                    subsumptionExpression.getSubConcept(),
                    idxExistential.getRole(),
                    idxExistential.getFiller())
            );
        }
    }
}
