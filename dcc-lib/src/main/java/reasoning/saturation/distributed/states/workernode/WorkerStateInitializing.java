package reasoning.saturation.distributed.states.workernode;

import networking.messages.AcknowledgementMessage;
import networking.messages.InitializeWorkerMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationWorker;

import java.util.ArrayList;
import java.util.List;

public class WorkerStateInitializing extends WorkerState {
    private long allWorkersInitializedMessageID;

    /**
     * Received axiom messages from other workers while in state 'initialized'.
     */
    private List<SaturationAxiomsMessage> bufferedAxiomMessages = new ArrayList<>();

    public WorkerStateInitializing(SaturationWorker partition) {
        super(partition);
    }

    @Override
    public void visit(InitializeWorkerMessage message) {
        log.info("Partition initialization message received from control node. Initializing partition...");
        this.worker.initializePartition(message);
        log.info("Partition successfully initialized.");

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
    public void visit(SaturationAxiomsMessage message) {
        this.bufferedAxiomMessages.add(message);
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());

        if (communicationChannel.allConnectionsEstablished()) {
            log.info("All connections to other workers successfully initialized.");
            this.communicationChannel.addInitialAxiomsToQueue();
            this.communicationChannel.addAxiomsToQueue(bufferedAxiomMessages);
            this.worker.switchState(new WorkerStateRunning(worker));

            communicationChannel.acknowledgeMessage(communicationChannel.getControlNodeID(), allWorkersInitializedMessageID);
        }
    }

}
