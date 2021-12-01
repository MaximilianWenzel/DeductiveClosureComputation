package reasoning.saturation.distributed.states.partitionnode;

import exceptions.MessageProtocolViolationException;
import networking.messages.*;
import reasoning.reasoner.IncrementalReasoner;
import reasoning.saturation.distributed.SaturationPartition;
import reasoning.saturation.distributed.communication.PartitionNodeCommunicationChannel;
import util.ConsoleUtils;

import java.util.logging.Logger;

public abstract class PartitionState implements MessageModelVisitor {

    protected final Logger log = ConsoleUtils.getLogger();
    protected SaturationPartition partition;
    protected PartitionNodeCommunicationChannel communicationChannel;
    protected IncrementalReasoner incrementalReasoner;

    public PartitionState(SaturationPartition partition) {
        this.partition = partition;
        this.communicationChannel = partition.getCommunicationChannel();
        this.incrementalReasoner = partition.getIncrementalReasoner();
    }

    public void mainPartitionLoop() throws InterruptedException {
        MessageModel messageModel = communicationChannel.read();
        messageModel.accept(this);
    }


    @Override
    public void visit(DebugMessage message) {
        log.info(message.getMessage());
    }

    @Override
    public void visit(InitializePartitionMessage message) {
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


    protected void messageProtocolViolation(StateInfoMessage message) {
        log.warning("State: " + this.getClass() + ", message type: " + message.getStatusMessage());
        throw new MessageProtocolViolationException();
    }
}
