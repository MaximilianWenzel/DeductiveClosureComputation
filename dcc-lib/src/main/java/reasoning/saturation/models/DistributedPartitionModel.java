package reasoning.saturation.models;

import networking.ServerData;
import reasoning.rules.Rule;
import reasoning.saturation.workloaddistribution.WorkloadDistributor;

import java.util.Collection;

public class DistributedPartitionModel extends PartitionModel {
    protected ServerData serverData;

    public DistributedPartitionModel(Collection<? extends Rule> rules,
                                     WorkloadDistributor workloadDistributor,
                                     ServerData serverData) {
        super(rules, workloadDistributor);
        this.serverData = serverData;
    }

    public ServerData getServerData() {
        return serverData;
    }
}
