package reasoning.saturation.models;

import data.Closure;
import networking.ServerData;
import reasoning.rules.Rule;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DistributedWorkerModel<C extends Closure<A>, A extends Serializable, T extends Serializable> extends WorkerModel<C, A, T> {
    protected ServerData serverData;

    public DistributedWorkerModel(List<? extends Rule<C, A>> rules, T workerTerms, ServerData serverData) {
        super(rules, workerTerms);
        this.serverData = serverData;
    }

    public ServerData getServerData() {
        return serverData;
    }
}
