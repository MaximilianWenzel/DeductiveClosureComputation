package eldlreasoning.rules;

import eldlreasoning.expressions.Expression;
import eldlreasoning.expressions.LinkExpression;
import eldlreasoning.expressions.SubsumptionExpression;
import eldlreasoning.models.IdxConcept;
import eldlreasoning.models.IdxExistential;
import eldlreasoning.premises.ELPremiseContext;

import java.util.Queue;
import java.util.Set;

/**
 * E ⊑ ∃S.D ⇐ E ⊑ ∃R.C ∧ C ⊑ D : R ⊑ₒ* S ∧ ∃S.D occurs negatively in ontology O.
 */
public class UnfoldReachabilityRule extends OWLELRule {
    // TODO implement rule using transitive closure of property hierarchy

    public UnfoldReachabilityRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        super(premiseContext, toDo);
    }

    @Override
    public void evaluate(Expression expression) {
        if (expression instanceof LinkExpression) {
            evaluate((LinkExpression) expression);
        }
    }

    public void evaluate(LinkExpression linkExpression) {
        IdxConcept e = linkExpression.getFirstConcept();
        IdxConcept c = linkExpression.getSecondConcept();
        Set<IdxConcept> supertypes = premiseContext.getSupertypeConcepts(c);
        for (IdxConcept d : supertypes) {
            IdxExistential existential = new IdxExistential(linkExpression.getRole(), d);
            if (premiseContext.isNegativeExistential(existential)) {
                toDo.add(new SubsumptionExpression(e, existential));
            }
        }
    }
}
