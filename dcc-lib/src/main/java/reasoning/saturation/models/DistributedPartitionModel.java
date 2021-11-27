package reasoning.saturation.models;

import networking.ServerData;
import reasoning.rules.Rule;

import java.util.Collection;
import java.util.Set;

public class DistributedPartitionModel extends PartitionModel {
    protected ServerData serverData;

    public DistributedPartitionModel(Collection<? extends Rule> rules, Set<?> partitionTerms, ServerData serverData) {
        super(rules, partitionTerms);
        this.serverData = serverData;
    }

    public ServerData getServerData() {
        return serverData;
    }
}
