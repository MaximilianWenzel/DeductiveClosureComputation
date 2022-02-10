package reasoning.saturation.models;

import data.Closure;
import networking.ServerData;

import java.io.Serializable;

public class DistributedWorkerModel<C extends Closure<A>, A extends Serializable> extends WorkerModel<C, A> {
    protected ServerData serverData;

    protected DistributedWorkerModel() {

    }

    public DistributedWorkerModel(WorkerModel<C, A> worker, ServerData serverData) {
        super(worker.id, worker.closure, worker.rules);
        this.serverData = serverData;
    }

    public ServerData getServerData() {
        return serverData;
    }
}
