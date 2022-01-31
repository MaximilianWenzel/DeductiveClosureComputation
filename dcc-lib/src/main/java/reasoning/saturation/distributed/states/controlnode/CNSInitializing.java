package reasoning.saturation.distributed.states.controlnode;

import data.Closure;
import enums.SaturationStatusMessage;
import enums.StatisticsComponent;
import networking.messages.AcknowledgementMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

import java.io.Serializable;

public class CNSInitializing<C extends Closure<A>, A extends Serializable, T extends Serializable>
        extends ControlNodeState<C, A, T> {

    protected int numberOfWorkers;

    public CNSInitializing(SaturationControlNode<C, A, T> saturationControlNode) {
        super(saturationControlNode);
        if (config.collectControlNodeStatistics()) {
            stats.startStopwatch(StatisticsComponent.CONTROL_NODE_INITIALIZING_ALL_WORKERS);
        }
        this.numberOfWorkers = saturationControlNode.getWorkers().size();
        this.saturationControlNode.initializeConnectionToWorkerServers();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case WORKER_SERVER_HELLO:
            case TODO_IS_EMPTY_EVENT:
                // ignore
                break;
            default:
                messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());
        log.info("ACK");
        log.info("Initialized workers: " + saturationControlNode.getInitializedWorkers().get());

        if (saturationControlNode.allWorkersInitialized()) {
            log.info("All workers successfully initialized.");
            saturationControlNode.broadcast(SaturationStatusMessage.CONTROL_NODE_INFO_ALL_WORKERS_INITIALIZED,
                    () -> {
                        // do nothing when message acknowledged
                    });
            if (config.collectControlNodeStatistics()) {
                stats.stopStopwatch(StatisticsComponent.CONTROL_NODE_INITIALIZING_ALL_WORKERS);
                stats.startStopwatch(StatisticsComponent.CONTROL_NODE_SATURATION_TIME);
            }

            // all workers initialized
            saturationControlNode.switchState(new CNSWaitingForWorkersToConverge<>(saturationControlNode));

            // distribute initial axioms
            saturationControlNode.distributeInitialAxioms();
        }
    }
}
