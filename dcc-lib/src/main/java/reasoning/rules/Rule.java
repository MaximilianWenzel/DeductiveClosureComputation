package reasoning.rules;

import data.Closure;

import java.io.Serializable;
import java.util.stream.Stream;

public abstract class Rule<C extends Closure<A>, A extends Serializable> implements Serializable {

    protected C closure;

    protected Rule() {

    }

    public abstract Stream<A> streamOfInferences(A axiom);

    public void setClosure(C closure) {
        this.closure = closure;
    }

}
