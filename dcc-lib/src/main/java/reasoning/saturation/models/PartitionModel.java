package reasoning.saturation.models;

import reasoning.rules.Rule;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class PartitionModel implements Serializable {

    private static final AtomicLong partitionIDCounter = new AtomicLong(1L);

    protected long id;
    protected Collection<? extends Rule> rules;
    protected Set<? extends Serializable> partitionTerms;

    public PartitionModel(Collection<? extends Rule> rules, Set<? extends Serializable> partitionTerms) {
        this.id = partitionIDCounter.getAndIncrement();
        this.rules = rules;
        this.partitionTerms = partitionTerms;
    }

    public Collection<? extends Rule> getRules() {
        return rules;
    }


    public long getID() {
        return id;
    }

    public Set<? extends Serializable> getPartitionTerms() {
        return partitionTerms;
    }
}
