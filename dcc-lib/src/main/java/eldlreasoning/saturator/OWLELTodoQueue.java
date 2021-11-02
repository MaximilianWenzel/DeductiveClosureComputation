package eldlreasoning.saturator;

import eldlreasoning.expressions.Expression;
import eldlreasoning.premises.ELPremiseContext;

import java.util.ArrayDeque;

public class OWLELTodoQueue extends ArrayDeque<Expression> {

    private ELPremiseContext premiseContext;

    public OWLELTodoQueue(ELPremiseContext premiseContext) {
        this.premiseContext = premiseContext;
    }

    @Override
    public boolean add(Expression e) {
        premiseContext.add(e);
        return super.add(e);
    }
}
