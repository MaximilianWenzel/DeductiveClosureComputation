package reasoning.saturation;

import data.Closure;

import java.io.Serializable;

/**
 * An interface for computing the deductive closure for a given set of axioms and rules.
 * @param <C> Type of the resulting deductive closure.
 * @param <A> Type of the axioms in the deductive closure.
 */
public interface Saturation<C extends Closure<A>, A extends Serializable> {

    /**
     * Compute the deductive closure for the given initial axioms and rules.
     * @return The computed deductive closure for the given initial axioms and rules.
     */
    C saturate();
}
