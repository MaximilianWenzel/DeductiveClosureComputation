package eldlreasoning.saturator;

import eldlreasoning.expressions.Expression;
import eldlreasoning.premises.ELPremiseContext;
import eldlreasoning.rules.*;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Collection;
import java.util.Queue;
import java.util.Set;

public class SingleThreadedELSaturator {

    private Set<Expression> closure = new UnifiedSet<>();
    private Queue<Expression> toDo;
    private ELPremiseContext premiseContext;
    private Collection<Rule> rules;

    public SingleThreadedELSaturator(Iterable<Expression> inputExpressions) {
        this.premiseContext = new ELPremiseContext(inputExpressions);
        this.toDo = new OWLELTodoQueue(premiseContext);
        inputExpressions.forEach(toDo::add);
        init();
    }

    private void init() {
        // initialize all required rules
        rules = new UnifiedSet<>();
        rules.add(new ComposeConjunctionRule(premiseContext, toDo));
        rules.add(new DecomposeConjunctionRule(premiseContext, toDo));
        rules.add(new DeriveInitFromReachabilityRule(premiseContext, toDo));
        rules.add(new DeriveLinkFromSubsumesExistentialRule(premiseContext, toDo));
        rules.add(new ReflexiveSubsumptionRule(premiseContext, toDo));
        rules.add(new SubsumedByBottomRule(premiseContext, toDo));
        rules.add(new SubsumedByTopRule(premiseContext, toDo));
        //rules.add(new TransitiveReachabilityClosureRule(premiseContext, toDo));
        rules.add(new UnfoldReachabilityRule(premiseContext, toDo));
        rules.add(new UnfoldSubsumptionRule(premiseContext, toDo));
    }

    public Set<Expression> saturate() {
        while (!toDo.isEmpty()) {
            process(toDo.remove());
        }
        return closure;
    }

    private void process(Expression expression) {
        if (closure.add(expression)) {
            for (Rule rule : rules) {
                // TODO implement more efficient rule application which considers expression type
                rule.evaluate(expression);
            }
        }
    }
}
