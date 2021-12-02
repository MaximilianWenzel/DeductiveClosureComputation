package networking.messages;

import reasoning.rules.Rule;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class InitializeWorkerMessage extends MessageModel {

    private long partitionID;
    private List<DistributedWorkerModel> partitions;
    private WorkloadDistributor workloadDistributor;
    private Collection<? extends Rule> rules;
    private Collection<? extends Serializable> initialAxioms;

    public InitializeWorkerMessage(long senderID,
                                   long partitionID,
                                   List<DistributedWorkerModel> partitions,
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

    public long getWorkerID() {
        return partitionID;
    }

    public List<DistributedWorkerModel> getPartitions() {
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
