package networking.messages;

import reasoning.rules.Rule;
import reasoning.saturation.models.DistributedPartitionModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class InitializePartitionMessage extends MessageModel {

    private long partitionID;
    private List<DistributedPartitionModel> partitions;
    private WorkloadDistributor workloadDistributor;
    private Collection<? extends Rule> rules;
    private Collection<? extends Serializable> initialAxioms;

    public InitializePartitionMessage(long senderID,
                                      long partitionID,
                                      List<DistributedPartitionModel> partitions,
                                      WorkloadDistributor workloadDistributor,
                                      Collection<? extends Rule> rules,
                                      Collection<? extends Serializable> initialAxioms) {
        super(senderID);
        this.partitionID = partitionID;
        this.partitions = partitions;
        this.workloadDistributor = workloadDistributor;
        this.rules = rules;
        this.initialAxioms = initialAxioms;
    }

    @Override
    public void accept(MessageModelVisitor visitor) {
        visitor.visit(this);
    }

    public long getPartitionID() {
        return partitionID;
    }

    public List<DistributedPartitionModel> getPartitions() {
        return partitions;
    }

    public WorkloadDistributor getWorkloadDistributor() {
        return workloadDistributor;
    }

    public Collection<? extends Serializable> getInitialAxioms() {
        return initialAxioms;
    }

    public Collection<? extends Rule> getRules() {
        return rules;
    }
}
