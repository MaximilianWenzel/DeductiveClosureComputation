package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.*;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;

public class WorkerStateConverged<C extends Closure<A>, A extends Serializable, T extends Serializable> extends WorkerState<C, A, T> {


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
                long sendClosureResultRequestMessageID = message.getMessageID();
                communicationChannel.acknowledgeMessage(communicationChannel.getControlNodeID(), sendClosureResultRequestMessageID);
                worker.switchState(new WorkerStateFinished<>(worker));
                break;
            default:
                messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(SaturationAxiomsMessage<C, A, T> message) {
        WorkerStateRunning<C, A, T> runningState = new WorkerStateRunning<>(worker);
        //log.info("Axioms received. Continuing saturation...");
        worker.switchState(runningState);
        runningState.visit(message);
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(RequestAxiomMessageCount message) {
        communicationChannel.setSaturationStage(message.getStage());
        communicationChannel.sendAxiomCountToControlNode();
    }
}
