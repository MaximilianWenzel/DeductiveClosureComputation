package reasoning.saturation.distributed.states.controlnode;

import data.Closure;
import enums.SaturationStatusMessage;
import networking.messages.AcknowledgementMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class CNSWaitingForWorkersToConverge<C extends Closure<A>, A extends Serializable, T extends Serializable> extends ControlNodeState<C, A, T> {

    protected AtomicInteger convergedWorkers = new AtomicInteger();
    protected int numberOfWorkers;

    public CNSWaitingForWorkersToConverge(SaturationControlNode<C, A, T> saturationControlNode) {
        super(saturationControlNode);
        this.numberOfWorkers = saturationControlNode.getWorkers().size();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case WORKER_INFO_SATURATION_CONVERGED:
                log.info("Worker " + message.getSenderID() + " converged.");
                convergedWorkers.getAndIncrement();
                communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());

                if (convergedWorkers.get() == numberOfWorkers) {
                    log.info("All workers converged.");
                    saturationControlNode.switchState(new CNSWaitingForClosureResults<>(saturationControlNode));
                    communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_REQUEST_SEND_CLOSURE_RESULT, new Runnable() {
                        @Override
                        public void run() {
                            communicationChannel.getReceivedClosureResultsCounter().getAndIncrement();
                            log.info("(" + communicationChannel.getReceivedClosureResultsCounter().get() +  "/" + numberOfWorkers + ")" +
                                    " workers have sent their closure results.");
                        }
                    });
                }
                break;
            case WORKER_INFO_SATURATION_RUNNING:
                log.info("Worker " + message.getSenderID() + " is running again.");
                convergedWorkers.getAndDecrement();
                communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
                break;
            default:
                messageProtocolViolation(message);
        }

    }

    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());
    }

}
