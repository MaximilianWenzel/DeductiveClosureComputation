package reasoning.saturation.distributed;

import data.Closure;
import reasoning.saturation.Saturation;
import reasoning.saturation.models.DistributedPartitionModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.List;

public class DistributedSaturation implements Saturation {

    protected WorkloadDistributor workloadDistributor;
    protected List<DistributedPartitionModel> partitions;
    protected SaturationControlNode controlNode;

    public DistributedSaturation(List<DistributedPartitionModel> partitions, WorkloadDistributor workloadDistributor, List<? extends Serializable> initialAxioms) {
        this.workloadDistributor = workloadDistributor;
        this.partitions = partitions;
        this.controlNode = new SaturationControlNode(partitions, workloadDistributor, initialAxioms);
    }

    @Override
    public Closure saturate() {
        return this.controlNode.saturate();
    }

}
