package reasoning.saturator.distributed;

import data.Dataset;
import networking.ClientComponent;
import networking.ServerData;
import networking.messages.SaturationAxiomsMessage;
import reasoning.rules.Rule;
import reasoning.saturator.PartitionModel;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class DistributedPartitionModel<P, T> extends PartitionModel<P, T> {

    public static final int INITIALIZED = 0;
    public static final int RUNNING_SATURATION = 1;
    public static final int FINISHED_SATURATION_WAITING_ON_CONTROL_NODE = 2;
    public static final int FINISHED = 3;

    protected final Object bufferedAxiomsLock = new Object();
    protected ServerData serverData;
    protected ClientComponent<P, T> clientComponent;
    protected List<P> bufferedAxioms = new ArrayList<>();

    protected int state;
    protected AtomicLong currentlyLargestStateSequenceNumber = new AtomicLong(0);

    public DistributedPartitionModel(Collection<? extends Rule<P>> rules,
                                     Set<T> termPartition,
                                     Dataset<P, T> datasetFragment,
                                     ServerData serverData) {
        super(rules, termPartition, datasetFragment);
        this.serverData = serverData;
    }

    public void initializeConnectionToPartitionNode(ClientComponent<P, T> clientComponent) {
        assert clientComponent.getRemotePortNumber() == serverData.getPortNumber();
        assert Objects.equals(clientComponent.getServerName(), serverData.getServerName());
        this.clientComponent = clientComponent;
        this.clientComponent.connectToServer();
    }

    public ServerData getServerData() {
        return serverData;
    }

    public ClientComponent<P, T> getClientComponent() {
        return clientComponent;
    }

    public synchronized void addAxiomToBuffer(P axiom) {
        synchronized (bufferedAxiomsLock) {
            bufferedAxioms.add(axiom);
        }
    }

    public synchronized void sendAxioms() {
        synchronized (bufferedAxiomsLock) {
            SaturationAxiomsMessage<P> data = new SaturationAxiomsMessage<>(bufferedAxioms);
            clientComponent.sendMessageAsync(data);
            bufferedAxioms = new ArrayList<>();
        }
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public AtomicLong getCurrentlyLargestStateSequenceNumber() {
        return currentlyLargestStateSequenceNumber;
    }
}
