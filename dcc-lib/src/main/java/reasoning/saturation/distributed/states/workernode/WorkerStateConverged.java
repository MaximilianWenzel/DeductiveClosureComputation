package reasoning.saturation.distributed.states.workernode;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.AcknowledgementMessage;
import networking.messages.InitializeWorkerMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationWorker;

public class WorkerStateConverged extends WorkerState {

    public WorkerStateConverged(SaturationWorker worker) {
        super(worker);
    }

    @Override
    public void visit(InitializeWorkerMessage message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        SaturationStatusMessage statusMessage = message.getStatusMessage();
        switch (statusMessage) {
            case CONTROL_NODE_REQUEST_SEND_CLOSURE_RESULT:
                communicationChannel.sendToControlNode(worker.getClosure());
                communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
                worker.switchState(new WorkerStateFinished(worker));
                break;
            default:
                messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(SaturationAxiomsMessage message) {
        long axiomSenderID = message.getSenderID();

        WorkerStateRunning runningState = new WorkerStateRunning(worker);
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
