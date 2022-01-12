package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import enums.StatisticsComponent;
import networking.messages.AcknowledgementMessage;
import networking.messages.InitializeWorkerMessage;
import networking.messages.RequestAxiomMessageCount;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WorkerStateInitializing<C extends Closure<A>, A extends Serializable, T extends Serializable> extends WorkerState<C, A, T> {
    private long allWorkersInitializedMessageID;
    private boolean allWorkersInitialized = false;

    /**
     * Received axiom messages from other workers while in state 'initialized'.
     */
    private List<A> bufferedAxiomMessages = new ArrayList<>();

    public WorkerStateInitializing(SaturationWorker<C, A, T> worker) {
        super(worker);
    }

    @Override
    public void visit(InitializeWorkerMessage<C, A, T> message) {
        log.info("Worker initialization message received from control node. Initializing worker...");
        this.worker.initializeWorker(message);
        this.config = this.worker.getConfig();
        this.stats = this.worker.getStats();
        log.info("Worker successfully initialized.");
        communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case CONTROL_NODE_INFO_ALL_WORKERS_INITIALIZED:
                log.info("Establishing connections to other workers...");

                if (config.collectWorkerNodeStatistics()) {
                    stats.startStopwatch(StatisticsComponent.WORKER_INITIALIZING_OTHER_WORKER_CONNECTIONS);
                }

                allWorkersInitializedMessageID = message.getMessageID();
                communicationChannel.connectToWorkerServers();
                allWorkersInitialized = true;
                break;
            case WORKER_SERVER_HELLO:
            case WORKER_CLIENT_HELLO:
                communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
                break;
            case CONTROL_NODE_REQUEST_SEND_CLOSURE_RESULT:
                WorkerStateConverged<C, A, T> stateConverged = new WorkerStateConverged<>(worker);
                this.worker.switchState(stateConverged);
                stateConverged.visit(message);
                break;
            default:
                messageProtocolViolation(message);
        }

        checkIfAllConnectionsEstablished();
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());
        checkIfAllConnectionsEstablished();
    }

    private void checkIfAllConnectionsEstablished() {
        if (communicationChannel.allConnectionsEstablished() && allWorkersInitialized) {
            if (config.collectWorkerNodeStatistics()) {
                stats.stopStopwatch(StatisticsComponent.WORKER_INITIALIZING_OTHER_WORKER_CONNECTIONS);
            }
            log.info("All connections to other workers successfully initialized.");
            this.communicationChannel.addInitialAxiomsToQueue();
            this.communicationChannel.addAxiomsToQueue(bufferedAxiomMessages);
            this.worker.switchState(new WorkerStateRunning<>(worker));

            if (config.collectWorkerNodeStatistics()) {
                stats.startStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
            }

            communicationChannel.acknowledgeMessage(communicationChannel.getControlNodeID(), allWorkersInitializedMessageID);
        }
    }

    @Override
    public void visit(RequestAxiomMessageCount message) {
        communicationChannel.getSaturationStageCounter().set(message.getStage());
    }

    @Override
    public void visit(A axiom) {
        bufferedAxiomMessages.add(axiom);
    }
}
