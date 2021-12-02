package reasoning.saturation.models;

import networking.ServerData;
import reasoning.rules.Rule;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

public class DistributedWorkerModel extends WorkerModel {
    protected ServerData serverData;

    public DistributedWorkerModel(Collection<? extends Rule> rules, Set<? extends Serializable> partitionTerms, ServerData serverData) {
        super(rules, partitionTerms);
        this.serverData = serverData;
    }

    public ServerData getServerData() {
        return serverData;
    }
}
