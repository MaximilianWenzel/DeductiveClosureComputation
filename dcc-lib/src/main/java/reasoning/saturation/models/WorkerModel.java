package reasoning.saturation.models;

import data.Closure;
import reasoning.rules.Rule;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class WorkerModel<C extends Closure<A>, A extends Serializable> implements Serializable {

    private static final AtomicLong workerIDCounter = new AtomicLong(1L);

    protected long id = workerIDCounter.getAndIncrement();
    protected Collection<? extends Rule<C, A>> rules;
    protected Set<? extends Serializable> workerTerms;

    public WorkerModel(Collection<? extends Rule<C, A>> rules, Set<? extends Serializable> workerTerms) {
        this.rules = rules;
        this.workerTerms = workerTerms;
    }

    public Collection<? extends Rule<C, A>> getRules() {
        return rules;
    }


    public long getID() {
        return id;
    }

    public Set<? extends Serializable> getWorkerTerms() {
        return workerTerms;
    }
}
