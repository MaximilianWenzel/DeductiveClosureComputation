package eldlreasoning.rules;

import eldlreasoning.expression.Expression;

public interface Rule {

    void evaluate(Expression expression);
}
