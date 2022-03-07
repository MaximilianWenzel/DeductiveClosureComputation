package reasoning.saturation.models;

import data.Closure;
import networking.ServerData;

import java.io.Serializable;

/**
 * In addition to the usual worker model representation, this class defines the appropriate server address and port number in order to be
 * able to establish a connection to the corresponding worker server.
 *
 */
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
