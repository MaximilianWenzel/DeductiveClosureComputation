package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import exceptions.MessageProtocolViolationException;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.messages.*;
import reasoning.reasoner.IncrementalReasoner;
import reasoning.saturation.distributed.SaturationWorker;
import reasoning.saturation.distributed.communication.WorkerNodeCommunicationChannel;
import util.ConsoleUtils;

import java.io.Serializable;
import java.util.logging.Logger;

public abstract class WorkerState<C extends Closure<A>, A extends Serializable> implements MessageModelVisitor<C, A> {

    protected final Logger log = ConsoleUtils.getLogger();
    protected SaturationWorker<C, A> worker;
    protected WorkerNodeCommunicationChannel<C, A> communicationChannel;
    protected IncrementalReasoner<C, A> incrementalReasoner;
    protected AcknowledgementEventManager acknowledgementEventManager;

    public WorkerState(SaturationWorker<C, A> worker) {
        this.worker = worker;
        this.communicationChannel = worker.getCommunicationChannel();
        this.incrementalReasoner = worker.getIncrementalReasoner();
        this.acknowledgementEventManager = communicationChannel.getAcknowledgementEventManager();
    }

    public void mainPartitionLoop() throws InterruptedException {
        Object message = communicationChannel.read();
        if (message instanceof MessageModel) {
            ((MessageModel<C, A>)message).accept(this);
        } else {
            throw new IllegalArgumentException("Axioms only allowed in state 'running'.");
        }
    }


    @Override
    public void visit(DebugMessage message) {
        log.info(message.getMessage());
    }

    @Override
    public void visit(InitializeWorkerMessage<C, A> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(SaturationAxiomsMessage<C, A> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        throw new MessageProtocolViolationException();
    }

    protected void messageProtocolViolation(StateInfoMessage message) {
        log.warning("State: " + this.getClass() + ", message type: " + message.getStatusMessage());
        throw new MessageProtocolViolationException();
    }
}
