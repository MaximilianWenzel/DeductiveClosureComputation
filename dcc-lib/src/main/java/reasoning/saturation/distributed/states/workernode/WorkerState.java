package reasoning.saturation.distributed.states.workernode;

import exceptions.MessageProtocolViolationException;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.messages.*;
import reasoning.reasoner.IncrementalReasoner;
import reasoning.saturation.distributed.SaturationWorker;
import reasoning.saturation.distributed.communication.WorkerNodeCommunicationChannel;
import util.ConsoleUtils;

import java.util.logging.Logger;

public abstract class WorkerState implements MessageModelVisitor {

    protected final Logger log = ConsoleUtils.getLogger();
    protected SaturationWorker worker;
    protected WorkerNodeCommunicationChannel communicationChannel;
    protected IncrementalReasoner incrementalReasoner;
    protected AcknowledgementEventManager acknowledgementEventManager;

    public WorkerState(SaturationWorker worker) {
        this.worker = worker;
        this.communicationChannel = worker.getCommunicationChannel();
        this.incrementalReasoner = worker.getIncrementalReasoner();
        this.acknowledgementEventManager = communicationChannel.getAcknowledgementEventManager();
    }

    public void mainPartitionLoop() throws InterruptedException {
        Object message = communicationChannel.read();
        if (message instanceof MessageModel) {
            ((MessageModel)message).accept(this);
        } else {
            throw new IllegalArgumentException("Axioms only allowed in state 'running'.");
        }
    }


    @Override
    public void visit(DebugMessage message) {
        log.info(message.getMessage());
    }

    @Override
    public void visit(InitializeWorkerMessage message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(SaturationAxiomsMessage message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        throw new MessageProtocolViolationException();
    }

    /*
    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getMessageID());
    }

     */

    protected void messageProtocolViolation(StateInfoMessage message) {
        log.warning("State: " + this.getClass() + ", message type: " + message.getStatusMessage());
        throw new MessageProtocolViolationException();
    }
}
