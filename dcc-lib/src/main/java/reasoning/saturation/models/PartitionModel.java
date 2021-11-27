package reasoning.saturation.models;

import reasoning.rules.Rule;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class PartitionModel {

    private static final AtomicLong partitionIDCounter = new AtomicLong(1L);

    protected long id;
    protected Collection<? extends Rule> rules;
    protected Set<?> partitionTerms;

    public PartitionModel(Collection<? extends Rule> rules, Set<?> partitionTerms) {
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

    public Set<?> getPartitionTerms() {
        return partitionTerms;
    }
}
