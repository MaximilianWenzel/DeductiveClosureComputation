package reasoning.rules;

import java.io.Serializable;

/**
 * An interface which determines how newly deduced conclusions from a given incremental reasoner are processed.
 * @param <A> Type of the newly deduced conclusion.
 */
public interface ConclusionProcessor<A extends Serializable> extends Serializable {
    void processConclusion(A axiom);
}
