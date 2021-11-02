package eldlreasoning.rules;

import eldlreasoning.expressions.Expression;
import eldlreasoning.expressions.SubsumptionExpression;
import eldlreasoning.models.IdxConcept;
import eldlreasoning.models.IdxConjunction;
import eldlreasoning.premises.ELPremiseContext;

import java.util.Queue;
import java.util.Set;

/**
 * C ⊑ D1 ⊓ D2 ⇐ C ⊑ D1 ∧ C ⊑ D2 : D1 ⊓ D2 occurs negatively in ontology O.
 */
public class ComposeConjunctionRule extends OWLELRule {

    public ComposeConjunctionRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        super(premiseContext, toDo);
    }

    @Override
    public void evaluate(Expression expression) {
        if (expression instanceof SubsumptionExpression) {
            evaluate((SubsumptionExpression) expression);
        }
    }


    public void evaluate(SubsumptionExpression subExpr) {
        Set<IdxConcept> supertypes = premiseContext.getSupertypeConcepts(subExpr.getSubConcept());
        for (IdxConcept supertype : supertypes) {
            if (premiseContext.isNegativeConjunction(subExpr.getSuperConcept(), supertype)) {
                IdxConjunction composedConj = new IdxConjunction(subExpr.getSuperConcept(), supertype);

                // add only if composed conjunction is used in ontology
                if (premiseContext.checkIfConceptIsUsedInOntology(composedConj)) {
                    this.toDo.add(new SubsumptionExpression(subExpr.getSubConcept(), composedConj));
                }
            }
        }
    }
}
