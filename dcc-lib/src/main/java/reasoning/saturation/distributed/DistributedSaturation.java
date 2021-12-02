package reasoning.saturation.distributed;

import data.Closure;
import reasoning.saturation.Saturation;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.List;

public class DistributedSaturation implements Saturation {

    protected WorkloadDistributor workloadDistributor;
    protected List<DistributedWorkerModel> partitions;
    protected SaturationControlNode controlNode;

    public DistributedSaturation(List<DistributedWorkerModel> partitions, WorkloadDistributor workloadDistributor, List<? extends Serializable> initialAxioms) {
        this.workloadDistributor = workloadDistributor;
        this.partitions = partitions;
        this.controlNode = new SaturationControlNode(partitions, workloadDistributor, initialAxioms);
    }

    @Override
    public Closure saturate() {
        return this.controlNode.saturate();
    }

}
