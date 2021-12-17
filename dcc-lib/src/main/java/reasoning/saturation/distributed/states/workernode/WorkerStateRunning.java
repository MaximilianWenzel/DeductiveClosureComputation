package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import exceptions.MessageProtocolViolationException;
import networking.messages.*;
import nio2kryo.Axiom;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;
import java.util.Collection;

public class WorkerStateRunning<C extends Closure<A>, A extends Serializable, T extends Serializable> extends WorkerState<C, A, T> {

    private boolean lastMessageWasAxiomCountRequest = false;

    public WorkerStateRunning(SaturationWorker<C, A, T> worker) {
        super(worker);
    }

    public void mainWorkerLoop() throws InterruptedException {
        if (!communicationChannel.hasMoreMessages()) {
            boolean axiomsSent = this.communicationChannel.sendAllBufferedAxioms();
            if (!lastMessageWasAxiomCountRequest || axiomsSent) {
                communicationChannel.sendAxiomCountToControlNode();
            }
            this.worker.switchState(new WorkerStateConverged<>(worker));
            return;
        }

        Object obj = communicationChannel.read();
        if (obj instanceof MessageModel) {
            ((MessageModel<C, A, T>)obj).accept(this);
        } else {
            incrementalReasoner.processAxiom((A) obj);
        }

        if (obj instanceof AxiomCount) {
            lastMessageWasAxiomCountRequest = true;
        } else {
            lastMessageWasAxiomCountRequest = false;
        }
    }


    @Override
    public void visit(InitializeWorkerMessage<C, A, T> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case WORKER_SERVER_HELLO:
            case WORKER_CLIENT_HELLO:
                communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
                break;
            default:
                messageProtocolViolation(message);
        }
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
        // ignore
    }

    @Override
    public void visit(RequestAxiomMessageCount message) {
        communicationChannel.setSaturationStage(message.getStage());
        communicationChannel.sendAxiomCountToControlNode();
    }

}

