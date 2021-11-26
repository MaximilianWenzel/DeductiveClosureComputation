package reasoning.reasoner;

import data.Closure;
import reasoning.rules.Rule;

import java.util.Collection;

public class IncrementalReasonerImpl implements IncrementalReasoner {

    private Collection<? extends Rule> rules;
    private Closure closure;

    public IncrementalReasonerImpl(Collection<? extends Rule> rules, Closure closure) {
        this.rules = rules;
        this.closure = closure;
    }

    @Override
    public void processAxiom(Object axiom) {
        if (closure.add(axiom)) {
            for (Rule rule : rules) {
                rule.apply(axiom);
            }
        }
    }
}
