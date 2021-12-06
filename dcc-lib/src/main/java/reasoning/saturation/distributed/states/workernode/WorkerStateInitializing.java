package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import networking.messages.AcknowledgementMessage;
import networking.messages.InitializeWorkerMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WorkerStateInitializing<C extends Closure<A>, A extends Serializable, T extends Serializable> extends WorkerState<C, A, T> {
    private long allWorkersInitializedMessageID;

    /**
     * Received axiom messages from other workers while in state 'initialized'.
     */
    private List<SaturationAxiomsMessage<C, A, T>> bufferedAxiomMessages = new ArrayList<>();

    public WorkerStateInitializing(SaturationWorker<C, A, T> worker) {
        super(worker);
    }

    @Override
    public void visit(InitializeWorkerMessage<C, A, T> message) {
        log.info("Worker initialization message received from control node. Initializing worker...");
        this.worker.initializeWorker(message);
        log.info("Worker successfully initialized.");

        this.communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case CONTROL_NODE_INFO_ALL_WORKERS_INITIALIZED:
                log.info("Establishing connections to other workers...");
                allWorkersInitializedMessageID = message.getMessageID();
                communicationChannel.connectToWorkerServers();
                break;
            case WORKER_SERVER_HELLO:
            case WORKER_CLIENT_HELLO:
                communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
                break;
            default:
                messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(SaturationAxiomsMessage<C, A, T> message) {
        this.bufferedAxiomMessages.add(message);
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());

        if (communicationChannel.allConnectionsEstablished()) {
            log.info("All connections to other workers successfully initialized.");
            this.communicationChannel.addInitialAxiomsToQueue();
            this.communicationChannel.addAxiomsToQueue(bufferedAxiomMessages);
            this.worker.switchState(new WorkerStateRunning<>(worker));

            communicationChannel.acknowledgeMessage(communicationChannel.getControlNodeID(), allWorkersInitializedMessageID);
        }
    }

}
