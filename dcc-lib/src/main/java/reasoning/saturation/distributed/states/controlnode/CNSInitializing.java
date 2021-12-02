package reasoning.saturation.distributed.states.controlnode;

import enums.SaturationStatusMessage;
import networking.messages.AcknowledgementMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

public class CNSInitializing extends ControlNodeState {

    protected int numberOfPartitions;

    public CNSInitializing(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getWorkers().size();
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
            // all partitions initialized
            saturationControlNode.switchState(new CNSWaitingForWorkersToConverge(saturationControlNode));
        }
    }
}
