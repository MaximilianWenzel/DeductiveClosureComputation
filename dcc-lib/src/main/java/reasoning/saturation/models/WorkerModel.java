package reasoning.saturation.models;

import data.Closure;
import reasoning.rules.Rule;

import java.io.Serializable;
import java.util.List;

/**
 * This class represents an internal representation of a given worker. It defines the worker ID which can be used in order to partition the
 * data, e.g., by using the hash-based partitioning approach, and the rules that are assigned to the respective worker.
 */
public class WorkerModel<C extends Closure<A>, A extends Serializable> implements Serializable {

    protected long id;
    protected List<? extends Rule<C, A>> rules;
    protected C closure;

    protected WorkerModel() {
    }

    public WorkerModel(long id, C closure, List<? extends Rule<C, A>> rules) {
        this.id = id;
        this.closure = closure;
        this.rules = rules;
    }

    public List<? extends Rule<C, A>> getRules() {
        return rules;
    }


    public long getID() {
        return id;
    }

    public C getClosure() {
        return closure;
    }
}
