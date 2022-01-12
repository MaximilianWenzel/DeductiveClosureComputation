package reasoning.reasoner;

import data.Closure;
import reasoning.rules.Rule;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

public class IncrementalStreamReasoner<C extends Closure<A>, A extends Serializable> {

    protected final Collection<? extends Rule<C, A>> rules;
    protected final Closure<A> closure;

    public IncrementalStreamReasoner(Collection<? extends Rule<C, A>> rules, Closure<A> closure) {
        this.rules = rules;
        this.closure = closure;
    }

    public Stream<A> processAxioms(Stream<A> axioms) {
        /*
        return axioms.filter(closure::add)
                .flatMap(axiom -> rules.stream().flatMap(rule -> rule.streamOfInferences(axiom)));

         */
        return null;
    }
}