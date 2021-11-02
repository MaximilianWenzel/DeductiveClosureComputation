package eldlreasoning.rules;

import eldlreasoning.expressions.Expression;
import eldlreasoning.expressions.InitExpression;
import eldlreasoning.expressions.SubsumptionExpression;
import eldlreasoning.models.IdxConcept;
import eldlreasoning.premises.ELPremiseContext;

import java.util.Queue;

/**
 * C ⊑ ⊤ ⇐ init(C) : ⊤ occurs negatively in ontology O.
 */
public class SubsumedByTopRule extends OWLELRule {

    private final IdxConcept top;

    public SubsumedByTopRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        super(premiseContext, toDo);
        this.top = this.premiseContext.getTop();
    }

    @Override
    public void evaluate(Expression expression) {
        if (expression instanceof InitExpression) {
            evaluate((InitExpression) expression);
        }
    }

    public void evaluate(InitExpression initExpression) {
        if (top.getNegOccurs().get() > 0) {
            toDo.add(new SubsumptionExpression(initExpression.getConcept(), top));
        }
    }
}
