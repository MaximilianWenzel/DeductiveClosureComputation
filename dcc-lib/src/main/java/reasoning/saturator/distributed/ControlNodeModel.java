package reasoning.saturator.distributed;
/*

import networking.ServerComponent;
import networking.messages.SaturationAxiomsMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
public class ControlNodeModel<P, T> {

    public static final int INITIALIZED = 0;
    public static final int WAITING_ON_PARTITION_NODES = 2;
    public static final int DISTRIBUTING_WORKLOAD = 1;

    protected final Object bufferedAxiomsLock = new Object();
    protected ServerComponent<P, T> connectionToControlNode;
    protected List<P> bufferedAxioms = new ArrayList<>();

    protected int state;
    protected AtomicLong currentlyLargestStateSequenceNumber = new AtomicLong(0);

    public ControlNodeModel(ServerComponent<P, T> connectionToControlNode) {
        this.connectionToControlNode = connectionToControlNode;
    }

    public ServerComponent<P, T> getConnectionToControlNode() {
        return connectionToControlNode;
    }

    public synchronized void addAxiomToBuffer(P axiom) {
        synchronized (bufferedAxiomsLock) {
            bufferedAxioms.add(axiom);
        }
    }

    public synchronized void sendAxioms() {
        synchronized (bufferedAxiomsLock) {
            SaturationAxiomsMessage<P> data = new SaturationAxiomsMessage<>(bufferedAxioms);
            connectionToControlNode.sendMessageAsync(data);
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

 */
