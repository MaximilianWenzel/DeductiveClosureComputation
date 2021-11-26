package networking.messages;

import reasoning.saturation.models.DistributedPartitionModel;
import reasoning.saturation.workloaddistribution.WorkloadDistributor;

import java.util.Collection;
import java.util.List;

public class InitializePartitionMessage extends MessageModel {

    private long partitionID;
    private List<DistributedPartitionModel> partitions;
    private WorkloadDistributor workloadDistributor;
    private Collection<Object> initialAxioms;

    public InitializePartitionMessage(long senderID, long partitionID,
                                      List<DistributedPartitionModel> partitions,
                                      WorkloadDistributor workloadDistributor,
                                      Collection<Object> initialAxioms) {
        super(senderID);
        this.partitionID = partitionID;
        this.partitions = partitions;
        this.workloadDistributor = workloadDistributor;
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

    public Collection<Object> getInitialAxioms() {
        return initialAxioms;
    }

}
