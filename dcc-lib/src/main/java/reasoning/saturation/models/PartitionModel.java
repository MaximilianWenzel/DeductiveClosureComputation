package reasoning.saturation.models;

import reasoning.rules.Rule;
import reasoning.saturation.workloaddistribution.WorkloadDistributor;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

public class PartitionModel {

    private static final AtomicLong partitionIDCounter = new AtomicLong(1L);

    protected long id;
    protected Collection<? extends Rule> rules;
    protected WorkloadDistributor workloadDistributor;

    public PartitionModel(Collection<? extends Rule> rules, WorkloadDistributor workloadDistributor) {
        this.id = partitionIDCounter.getAndIncrement();
        this.rules = rules;
        this.workloadDistributor = workloadDistributor;
    }

    public Collection<? extends Rule> getRules() {
        return rules;
    }

    public WorkloadDistributor getWorkloadDistributor() {
        return workloadDistributor;
    }

    public long getId() {
        return id;
    }
}
