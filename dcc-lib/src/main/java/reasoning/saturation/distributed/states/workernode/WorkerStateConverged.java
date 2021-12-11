package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.AcknowledgementMessage;
import networking.messages.InitializeWorkerMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;

public class WorkerStateConverged<C extends Closure<A>, A extends Serializable, T extends Serializable> extends WorkerState<C, A, T> {


    boolean closureHasBeenSentToControlNode = false;

    //This message is acknowledged only if all closure results have been sent to and acknowledged by the control node.
    long sendClosureResultRequestMessageID = -1L;

    public WorkerStateConverged(SaturationWorker<C, A, T> worker) {
        super(worker);
    }

    @Override
    public void visit(InitializeWorkerMessage<C, A, T> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        SaturationStatusMessage statusMessage = message.getStatusMessage();
        switch (statusMessage) {
            case CONTROL_NODE_REQUEST_SEND_CLOSURE_RESULT:
                communicationChannel.sendToControlNode(worker.getClosure());
                sendClosureResultRequestMessageID = message.getMessageID();
                closureHasBeenSentToControlNode = true;
                break;
            default:
                messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(SaturationAxiomsMessage<C, A, T> message) {
        long axiomSenderID = message.getSenderID();

        WorkerStateRunning<C, A, T> runningState = new WorkerStateRunning<>(worker, message.getMessageID());
        log.info("Axioms received. Continuing saturation...");
        worker.switchState(runningState);
        communicationChannel.sendToControlNode(
                SaturationStatusMessage.WORKER_INFO_SATURATION_RUNNING,
                () -> communicationChannel.acknowledgeMessage(axiomSenderID, message.getMessageID())
        );
        runningState.visit(message);
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        // status message "converged" from worker is acknowledged by control node
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());

        if (closureHasBeenSentToControlNode
                && communicationChannel.getDistributedMessages().get() == communicationChannel.getAcknowledgedMessages().get()) {
            communicationChannel.acknowledgeMessage(communicationChannel.getControlNodeID(), sendClosureResultRequestMessageID);
            worker.switchState(new WorkerStateFinished<>(worker));
        }
    }

}
