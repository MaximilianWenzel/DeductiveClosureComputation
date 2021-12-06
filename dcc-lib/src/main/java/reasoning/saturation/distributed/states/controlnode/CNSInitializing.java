package reasoning.saturation.distributed.states.controlnode;

import data.Closure;
import enums.SaturationStatusMessage;
import networking.messages.AcknowledgementMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

import java.io.Serializable;

public class CNSInitializing<C extends Closure<A>, A extends Serializable, T extends Serializable> extends ControlNodeState<C, A, T> {

    protected int numberOfWorkers;

    public CNSInitializing(SaturationControlNode<C, A, T> saturationControlNode) {
        super(saturationControlNode);
        this.numberOfWorkers = saturationControlNode.getWorkers().size();
        this.communicationChannel.initializeConnectionToWorkerServers();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case WORKER_SERVER_HELLO:
                communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
                break;
            default:
                messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());

        if (communicationChannel.allWorkersInitialized()) {
            log.info("All workers successfully initialized.");
            communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_INFO_ALL_WORKERS_INITIALIZED,
                    new Runnable() {
                        @Override
                        public void run() {
                            // do nothing when message acknowledged
                        }
                    });
            // all workers initialized
            saturationControlNode.switchState(new CNSWaitingForWorkersToConverge<>(saturationControlNode));
        }
    }
}
