package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.AcknowledgementMessage;
import networking.messages.InitializeWorkerMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;

public class WorkerStateFinished<C extends Closure<A>, A extends Serializable, T extends Serializable>
        extends WorkerState<C, A, T> {

    public WorkerStateFinished(SaturationWorker<C, A, T> worker) {
        super(worker);
    }


    @Override
    public void visit(InitializeWorkerMessage<C, A, T> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        if (!message.getStatusMessage().equals(SaturationStatusMessage.TODO_IS_EMPTY_EVENT)) {
            messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(A axiom) {
        throw new MessageProtocolViolationException();
    }
}
