package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.*;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;
import java.util.Collection;

public class WorkerStateRunning<C extends Closure<A>, A extends Serializable, T extends Serializable> extends WorkerState<C, A, T> {

    private long messageIDOfAxiomsReceivedInConvergedState = -1L;

    public WorkerStateRunning(SaturationWorker<C, A, T> worker) {
        super(worker);
    }
    public WorkerStateRunning(SaturationWorker<C, A, T> worker, long messageIDOfAxiomsReceivedInConvergedState) {
        super(worker);
        this.messageIDOfAxiomsReceivedInConvergedState = messageIDOfAxiomsReceivedInConvergedState;
    }

    public void mainWorkerLoop() throws InterruptedException {
        if (!communicationChannel.hasMoreMessagesToReadWriteOrToBeAcknowledged()) {
            boolean axiomsTransmitted = this.communicationChannel.sendAllBufferedAxioms();
            if (axiomsTransmitted) {
                return;
            }

            communicationChannel.send(communicationChannel.getControlNodeID(),
                    SaturationStatusMessage.WORKER_INFO_SATURATION_CONVERGED,
                    new Runnable() {
                        @Override
                        public void run() {
                            // do nothing
                        }
                    });

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
        // acknowledge message only if it has not been acknowledged before
        if (message.getMessageID() != messageIDOfAxiomsReceivedInConvergedState) {
            communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
        }

        Collection<A> axioms = message.getAxioms();
        for (A axiom : axioms) {
            incrementalReasoner.processAxiom(axiom);

        }
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());
    }
}

