package eldlreasoning.rules;

import eldlreasoning.expressions.Expression;
import eldlreasoning.premises.ELPremiseContext;

import java.util.Queue;

public abstract class OWLELRule implements Rule {
    protected Queue<Expression> toDo;
    protected ELPremiseContext premiseContext;

    public OWLELRule(ELPremiseContext premiseContext, Queue<Expression> toDo) {
        this.premiseContext = premiseContext;
        this.toDo = toDo;
    }
}
