package reasoning.saturation.models;

import data.Closure;
import networking.ServerData;
import reasoning.rules.Rule;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

public class DistributedWorkerModel<C extends Closure<A>, A extends Serializable> extends WorkerModel<C, A> {
    protected ServerData serverData;

    public DistributedWorkerModel(Collection<? extends Rule<C, A>> rules, Set<? extends Serializable> partitionTerms, ServerData serverData) {
        super(rules, partitionTerms);
        this.serverData = serverData;
    }

    public ServerData getServerData() {
        return serverData;
    }
}
