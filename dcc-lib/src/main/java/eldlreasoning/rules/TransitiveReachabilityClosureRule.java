package eldlreasoning.rules;

import eldlreasoning.expressions.Expression;
import eldlreasoning.premises.ELPremiseContext;

import java.util.Queue;

/**
 * E ⊑ ∃S.D ⇐ E ⊑ ∃R1.C ∧ C ⊑ ∃R2.D : R1 ⊑*ₒ S1 ∧ R2 ⊑*ₒ S2 ∧ S1 ∘ S2 ⊑ S ∈ O
 */
public class TransitiveReachabilityClosureRule extends OWLELRule {
    public TransitiveReachabilityClosureRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        super(premiseContext, toDo);
    }

    // TODO implement rule using transitive closure of property hierarchy
    @Override
    public void evaluate(Expression expression) {

    }
}
