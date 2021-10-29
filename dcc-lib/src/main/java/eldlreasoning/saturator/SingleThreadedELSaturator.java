package eldlreasoning.saturator;

import eldlreasoning.expression.Expression;
import eldlreasoning.premise.ELPremiseContext;
import eldlreasoning.rules.Rule;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.Set;

public class SingleThreadedELSaturator {

    private Set<Expression> closure = new UnifiedSet<>();
    private Queue<Expression> toDo = new ArrayDeque<>();
    private ELPremiseContext premiseContext;
    private Collection<Rule> rules;

    public SingleThreadedELSaturator(Iterable<Expression> inputExpressions, Collection<Rule> rules) {
        this.rules = rules;
        this.premiseContext = new ELPremiseContext(inputExpressions);
        inputExpressions.forEach(toDo::add);
    }

    public void startSaturation() {
        while (!toDo.isEmpty()) {
            process(toDo.remove());
        }
    }

    private void process(Expression expression) {
        for (Rule rule : rules) {
            rule.evaluate(expression);
        }
    }
}
