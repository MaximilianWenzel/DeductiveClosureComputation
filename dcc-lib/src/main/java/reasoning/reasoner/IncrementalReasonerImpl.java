package reasoning.reasoner;

import data.Closure;
import reasoning.rules.Rule;

import java.io.Serializable;
import java.util.Collection;

public class IncrementalReasonerImpl<C extends Closure<A>, A extends Serializable> implements IncrementalReasoner<C, A> {

    protected final Collection<? extends Rule<C, A>> rules;
    protected final Closure<A> closure;

    public IncrementalReasonerImpl(Collection<? extends Rule<C, A>> rules, Closure<A> closure) {
        this.rules = rules;
        this.closure = closure;
    }

    @Override
    public void processAxiom(A axiom) {
        if (closure.add(axiom)) {
            for (Rule<C, A> rule : rules) {
                rule.apply(axiom);
            }
        }
    }

}
