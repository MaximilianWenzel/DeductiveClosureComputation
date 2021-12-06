package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import exceptions.MessageProtocolViolationException;
import networking.messages.AcknowledgementMessage;
import networking.messages.InitializeWorkerMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;

public class WorkerStateFinished<C extends Closure<A>, A extends Serializable, T extends Serializable> extends WorkerState<C, A, T> {

    public WorkerStateFinished(SaturationWorker<C, A, T> worker) {
        super(worker);
    }


    @Override
    public void visit(InitializeWorkerMessage<C, A, T> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        messageProtocolViolation(message);
    }

    @Override
    public void visit(SaturationAxiomsMessage<C, A, T> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        throw new MessageProtocolViolationException();
    }
}
