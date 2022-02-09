package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import exceptions.MessageProtocolViolationException;
import networking.messages.*;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;

public class WorkerStateSendingClosureResults<C extends Closure<A>, A extends Serializable, T extends Serializable> extends WorkerState<C, A, T> {

    private long sendClosureResultMessageID;
    private boolean closureResultRequestAcknowledged = false;

    public WorkerStateSendingClosureResults(SaturationWorker<C, A, T> worker, long sendClosureResultMessageID) {
        super(worker);
        this.sendClosureResultMessageID = sendClosureResultMessageID;
        communicationChannel.addClosureAxiomsToToDo(worker.getClosure());
    }

    public void mainWorkerLoop(Object obj) {
        if (obj instanceof MessageModel) {
            ((MessageModel<C, A, T>)obj).accept(this);
        } else {
            visit((A)obj);
        }
    }

    public void onToDoIsEmpty() {
        if (!closureResultRequestAcknowledged) {
            closureResultRequestAcknowledged = true;
            communicationChannel.acknowledgeMessage(
                    communicationChannel.getControlNodeID(),
                    sendClosureResultMessageID
            );
        }
    }

    @Override
    public void visit(InitializeWorkerMessage<C, A, T> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case CONTROL_NODE_INFO_CLOSURE_RESULTS_RECEIVED:
                log.info("Control node received all closure results. Saturation finished.");
                worker.switchState(new WorkerStateFinished<>(worker));
                break;
            default:
                messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());
    }

    @Override
    public void visit(RequestAxiomMessageCount message) {
        communicationChannel.getSaturationStageCounter().set(message.getStage());
        communicationChannel.sendAxiomCountToControlNode();
    }

    @Override
    public void visit(A axiom) {
        communicationChannel.sendToControlNode(axiom);
    }

}

