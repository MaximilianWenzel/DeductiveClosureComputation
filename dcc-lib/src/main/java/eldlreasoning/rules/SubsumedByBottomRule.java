package eldlreasoning.rules;

import eldlreasoning.expression.Expression;
import eldlreasoning.expression.LinkExpression;
import eldlreasoning.expression.SubsumptionExpression;
import eldlreasoning.models.IdxConcept;
import eldlreasoning.premise.ELPremiseContext;

import java.util.Queue;
import java.util.Set;

/**
 * E ⊑ ⊥ ⇐ E ⊑ ∃R.C ∧ C ⊑ ⊥
 */
public class SubsumedByBottomRule implements Rule {
    private final IdxConcept bottom;
    private final Queue<Expression> toDo;
    private final ELPremiseContext premiseContext;

    public SubsumedByBottomRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        this.premiseContext = premiseContext;
        this.bottom = premiseContext.getTop();
        this.toDo = toDo;
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
