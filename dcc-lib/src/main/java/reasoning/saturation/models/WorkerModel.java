package reasoning.saturation.models;

import reasoning.rules.Rule;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class WorkerModel implements Serializable {

    private static final AtomicLong workerIDCounter = new AtomicLong(1L);

    protected long id = workerIDCounter.getAndIncrement();
    protected Collection<? extends Rule> rules;
    protected Set<? extends Serializable> workerTerms;

    public WorkerModel(Collection<? extends Rule> rules, Set<? extends Serializable> workerTerms) {
        this.rules = rules;
        this.workerTerms = workerTerms;
    }

    public Collection<? extends Rule> getRules() {
        return rules;
    }


    public long getID() {
        return id;
    }

    public Set<? extends Serializable> getWorkerTerms() {
        return workerTerms;
    }
}
