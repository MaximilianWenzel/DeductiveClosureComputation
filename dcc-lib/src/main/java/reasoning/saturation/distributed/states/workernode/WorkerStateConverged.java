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

public class WorkerStateConverged<C extends Closure<A>, A extends Serializable> extends WorkerState<C, A> {

    public WorkerStateConverged(SaturationWorker<C, A> worker) {
        super(worker);
    }

    @Override
    public void visit(InitializeWorkerMessage<C, A> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        SaturationStatusMessage statusMessage = message.getStatusMessage();
        switch (statusMessage) {
            case CONTROL_NODE_REQUEST_SEND_CLOSURE_RESULT:
                communicationChannel.sendToControlNode(worker.getClosure());
                communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
                worker.switchState(new WorkerStateFinished<>(worker));
                break;
            default:
                messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(SaturationAxiomsMessage<C, A> message) {
        long axiomSenderID = message.getSenderID();

        WorkerStateRunning<C, A> runningState = new WorkerStateRunning<>(worker);
        log.info("Axioms received. Continuing saturation...");
        worker.switchState(runningState);
        communicationChannel.sendToControlNode(SaturationStatusMessage.WORKER_INFO_SATURATION_RUNNING, new Runnable() {
            @Override
            public void run() {
                communicationChannel.acknowledgeMessage(axiomSenderID, message.getMessageID());
            }
        });
        runningState.visit(message);
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        // status message "converged" from worker is acknowledged by control node
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());
    }

}
