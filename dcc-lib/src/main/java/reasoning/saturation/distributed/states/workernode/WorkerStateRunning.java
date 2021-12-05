package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.*;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;
import java.util.Collection;

public class WorkerStateRunning<C extends Closure<A>, A extends Serializable> extends WorkerState<C, A> {

    public WorkerStateRunning(SaturationWorker<C, A> worker) {
        super(worker);
    }

    public void mainPartitionLoop() throws InterruptedException {
        if (!communicationChannel.hasMoreMessagesToReadWriteOrToBeAcknowledged()) {
            //boolean axiomsTransmitted = this.communicationChannel.sendAllBufferedAxioms();
            /*
            if (!axiomsTransmitted) {
            }

             */
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
            ((MessageModel<C, A>)obj).accept(this);
        } else {
            incrementalReasoner.processAxiom((A) obj);
        }
    }


    @Override
    public void visit(InitializeWorkerMessage<C, A> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        messageProtocolViolation(message);
    }

    @Override
    public void visit(SaturationAxiomsMessage<C, A> message) {
        Collection<A> axioms = message.getAxioms();
        for (A axiom : axioms) {
            incrementalReasoner.processAxiom(axiom);
        }
        communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());
    }
}

