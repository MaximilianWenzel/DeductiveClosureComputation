package reasoning.saturation.distributed.states.workernode;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.*;
import reasoning.saturation.distributed.SaturationWorker;

import java.util.Collection;

public class WorkerStateRunning extends WorkerState {

    public WorkerStateRunning(SaturationWorker partition) {
        super(partition);
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

            this.worker.switchState(new WorkerStateConverged(worker));
            return;
        }

        Object obj = communicationChannel.read();
        if (obj instanceof MessageModel) {
            ((MessageModel)obj).accept(this);
        } else {
            incrementalReasoner.processAxiom(obj);
        }
    }


    @Override
    public void visit(InitializeWorkerMessage message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        messageProtocolViolation(message);
    }

    @Override
    public void visit(SaturationAxiomsMessage message) {
        Collection<?> axioms = message.getAxioms();
        for (Object axiom : axioms) {
            incrementalReasoner.processAxiom(axiom);
        }
        communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());
    }
}

