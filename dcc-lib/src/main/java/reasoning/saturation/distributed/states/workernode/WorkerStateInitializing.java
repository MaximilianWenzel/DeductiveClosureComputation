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
        worker.acknowledgeMessage(message.getSenderID(), message.getMessageID());
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
                worker.connectToWorkerServers();
                allWorkersInitialized = true;
                break;
            case WORKER_SERVER_HELLO:
                // do nothing
                break;
            case WORKER_CLIENT_HELLO:
                worker.acknowledgeMessage(message.getSenderID(), message.getMessageID());
                break;
            case CONTROL_NODE_REQUEST_SEND_CLOSURE_RESULT:
                WorkerStateConverged<C, A, T> stateConverged = new WorkerStateConverged<>(worker);
                this.worker.switchState(stateConverged);
                stateConverged.visit(message);
                break;
            case TODO_IS_EMPTY_EVENT:
                // ignore
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
        if (worker.allConnectionsEstablished() && allWorkersInitialized) {
            if (config.collectWorkerNodeStatistics()) {
                stats.stopStopwatch(StatisticsComponent.WORKER_INITIALIZING_OTHER_WORKER_CONNECTIONS);
            }
            log.info("All connections to other workers successfully initialized.");
            this.worker.switchState(new WorkerStateRunning<>(worker));
            this.worker.addAxiomsToToDoQueue(bufferedAxiomMessages);
            worker.acknowledgeMessage(worker.getControlNodeID(), allWorkersInitializedMessageID);
        }
    }

    @Override
    public void visit(RequestAxiomMessageCount message) {
        worker.getSaturationStageCounter().set(message.getStage());
    }

    @Override
    public void visit(A axiom) {
        bufferedAxiomMessages.add(axiom);
    }
}
