package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import exceptions.MessageProtocolViolationException;
import networking.messages.*;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;
import java.util.Collection;

public class WorkerStateRunning<C extends Closure<A>, A extends Serializable, T extends Serializable> extends WorkerState<C, A, T> {

    public WorkerStateRunning(SaturationWorker<C, A, T> worker) {
        super(worker);
    }

    public void mainWorkerLoop() throws InterruptedException {
        if (!communicationChannel.hasMoreMessages()) {
            this.communicationChannel.sendAllBufferedAxioms();
            communicationChannel.sendAxiomCountToControlNode();

            this.worker.switchState(new WorkerStateConverged<>(worker));
            return;
        }

        Object obj = communicationChannel.read();
        if (obj instanceof MessageModel) {
            ((MessageModel<C, A, T>)obj).accept(this);
        } else {
            incrementalReasoner.processAxiom((A) obj);
        }
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
        communicationChannel.getReceivedAxiomMessages().getAndIncrement();

        Collection<A> axioms = message.getAxioms();
        for (A axiom : axioms) {
            incrementalReasoner.processAxiom(axiom);
        }
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

