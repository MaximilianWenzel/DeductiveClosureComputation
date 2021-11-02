package eldlreasoning.rules;

import eldlreasoning.expressions.Expression;
import eldlreasoning.expressions.SubsumptionExpression;
import eldlreasoning.models.IdxConcept;
import eldlreasoning.premises.ELPremiseContext;

import java.util.Queue;
import java.util.Set;

/**
 * C ⊑ E ⇐ C ⊑ D : D ⊑ E ∈ O
 */
public class UnfoldSubsumptionRule extends OWLELRule {

    public UnfoldSubsumptionRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        super(premiseContext, toDo);
    }

    @Override
    public void evaluate(Expression expression) {
        if (expression instanceof SubsumptionExpression) {
            evaluate((SubsumptionExpression) expression);
        }
    }

    public void evaluate(SubsumptionExpression subsumptionExpression) {
        IdxConcept c = subsumptionExpression.getSubConcept();
        IdxConcept d = subsumptionExpression.getSuperConcept();
        Set<IdxConcept> supertypes = premiseContext.getSupertypeConcepts(d);
        for (IdxConcept e : supertypes) {
            toDo.add(new SubsumptionExpression(c, e));
        }
    }
}
