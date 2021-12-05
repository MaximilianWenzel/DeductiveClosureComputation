package reasoning.reasoner;

import data.Closure;

import java.io.Serializable;

public interface IncrementalReasoner<C extends Closure<A>, A extends Serializable> {

    void processAxiom(A axiom);
}
