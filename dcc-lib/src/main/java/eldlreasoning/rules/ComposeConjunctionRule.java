package eldlreasoning.rules;

import eldlreasoning.expression.Expression;
import eldlreasoning.expression.SubsumptionExpression;
import eldlreasoning.models.IdxConcept;
import eldlreasoning.models.IdxConjunction;
import eldlreasoning.premise.ELPremiseContext;

import java.util.Queue;
import java.util.Set;

/**
 * C ⊑ D1 ⊓ D2 ⇐ C ⊑ D1 ∧ C ⊑ D2 : D1 ⊓ D2 occurs negatively in ontology O.
 */
public class ComposeConjunctionRule implements Rule {
    private final Queue<Expression> toDo;
    private final ELPremiseContext premiseContext;

    public ComposeConjunctionRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
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
        Set<IdxConcept> supertypes = premiseContext.getSupertypeConcepts(subExpr.getFirstConcept());
        for (IdxConcept supertype : supertypes) {
            if (premiseContext.isNegativeConjunction(subExpr.getSecondConcept(), supertype)) {
                IdxConjunction composedConj = new IdxConjunction(subExpr.getSecondConcept(), supertype);

                // add only if composed conjunction is used in ontology
                if (premiseContext.checkIfConceptIsUsedInOntology(composedConj)) {
                    this.toDo.add(new SubsumptionExpression(subExpr.getFirstConcept(), composedConj));
                }
            }
        }
    }
}
