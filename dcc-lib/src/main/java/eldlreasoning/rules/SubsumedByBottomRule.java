package eldlreasoning.rules;

import eldlreasoning.expressions.Expression;
import eldlreasoning.expressions.LinkExpression;
import eldlreasoning.expressions.SubsumptionExpression;
import eldlreasoning.models.IdxConcept;
import eldlreasoning.premises.ELPremiseContext;

import java.util.Queue;
import java.util.Set;

/**
 * E ⊑ ⊥ ⇐ E ⊑ ∃R.C ∧ C ⊑ ⊥
 */
public class SubsumedByBottomRule extends OWLELRule {
    private final IdxConcept bottom;

    public SubsumedByBottomRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        super(premiseContext, toDo);
        this.bottom = premiseContext.getBottom();
    }

    @Override
    public void evaluate(Expression expression) {
        if (expression instanceof LinkExpression) {
            evaluate((LinkExpression) expression);
        }
    }

    public void evaluate(LinkExpression linkExpression) {
        IdxConcept reachableConcept = linkExpression.getSecondConcept();
        Set<IdxConcept> supertypes = premiseContext.getSupertypeConcepts(reachableConcept);
        if (supertypes.contains(bottom)) {
            toDo.add(new SubsumptionExpression(linkExpression.getFirstConcept(), bottom));
        }
    }
}
