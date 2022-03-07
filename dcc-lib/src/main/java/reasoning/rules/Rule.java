package reasoning.rules;

import data.Closure;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * This class is a representation of an inference rule which can be used in order to derive from given axioms new conclusions.
 *
 * @param <C> Type of the closure object whose axioms are used to derive new conclusions.
 * @param <A> Type of the axioms in the closure object.
 */
public abstract class Rule<C extends Closure<A>, A extends Serializable> implements Serializable {

    protected C closure;

    protected Rule() {

    }

    /**
     * Returns a stream of conclusions which are deduced by instantiating the rule with the given axiom and other axioms from the current
     * state of the deductive closure.
     *
     * @param axiom Axiom which is used to instantiate the given rule.
     * @return All conclusions which can be deduced by the rule from the given axiom in combination with other axioms from the closure
     * object.
     */
    public abstract Stream<A> streamOfConclusions(A axiom);

    public void setClosure(C closure) {
        this.closure = closure;
    }

}
