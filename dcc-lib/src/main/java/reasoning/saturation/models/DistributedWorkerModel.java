package reasoning.saturation.models;

import data.Closure;
import networking.ServerData;

import java.io.Serializable;

public class DistributedWorkerModel<C extends Closure<A>, A extends Serializable, T extends Serializable> extends WorkerModel<C, A, T> {
    protected ServerData serverData;

    protected DistributedWorkerModel() {

    }

    public DistributedWorkerModel(WorkerModel<C, A, T> worker, ServerData serverData) {
        super(worker.closure, worker.rules, worker.workerTerms);
        this.serverData = serverData;
        this.id = worker.id;
    }

    public ServerData getServerData() {
        return serverData;
    }
}
