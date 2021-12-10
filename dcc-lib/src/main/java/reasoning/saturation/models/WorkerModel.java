package reasoning.saturation.models;

import data.Closure;
import reasoning.rules.Rule;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class WorkerModel<C extends Closure<A>, A extends Serializable, T extends Serializable> implements Serializable {

    private static final AtomicLong workerIDCounter = new AtomicLong(1L);

    protected long id = workerIDCounter.getAndIncrement();
    protected List<? extends Rule<C, A>> rules;
    protected T workerTerms;

    public WorkerModel(List<? extends Rule<C, A>> rules, T workerTerms) {
        this.rules = rules;
        this.workerTerms = workerTerms;
    }

    public List<? extends Rule<C, A>> getRules() {
        return rules;
    }


    public long getID() {
        return id;
    }

    public T getWorkerTerms() {
        return workerTerms;
    }
}
