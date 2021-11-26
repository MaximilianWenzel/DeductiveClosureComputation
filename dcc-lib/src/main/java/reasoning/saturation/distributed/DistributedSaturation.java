package reasoning.saturation.distributed;

import data.Closure;
import reasoning.rules.Rule;
import reasoning.saturation.models.PartitionModel;
import reasoning.saturation.Saturation;
import reasoning.saturation.workloaddistribution.WorkloadDistributor;

import java.util.Collection;
import java.util.List;

public class DistributedSaturation implements Saturation {

    protected WorkloadDistributor workloadDistributor;
    protected List<PartitionModel> partitions;
    protected SaturationControlNode controlNode;

    public DistributedSaturation(WorkloadDistributor workloadDistributor, List<PartitionModel> partitions) {
        this.workloadDistributor = workloadDistributor;
        this.partitions = partitions;
        this.controlNode = new SaturationControlNode(partitions);
    }

    @Override
    public Closure saturate() {
        return this.controlNode.saturate();
    }

    @Override
    public void setRules(Collection<? extends Rule> rules) {
        // TODO implement
    }

}
