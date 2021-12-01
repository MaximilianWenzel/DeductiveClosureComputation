package reasoning.saturation.distributed.states.partitionnode;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.InitializePartitionMessage;
import networking.messages.MessageModel;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationPartition;

import java.util.Collection;

public class PartitionStateRunning extends PartitionState {
    public PartitionStateRunning(SaturationPartition partition) {
        super(partition);
    }

    public void mainPartitionLoop() throws InterruptedException {
        if (!communicationChannel.hasMoreMessages()) {
            // TODO inefficient
            communicationChannel.sendAllBufferedAxioms();
            Thread.sleep(100);
        }
        if (!communicationChannel.hasMoreMessages()) {
            communicationChannel.sendToControlNode(SaturationStatusMessage.PARTITION_INFO_SATURATION_CONVERGED);

            partition.switchState(new PartitionStateConverged(partition));
        }

        MessageModel messageModel = communicationChannel.read();
        messageModel.accept(this);
    }

    @Override
    public void visit(InitializePartitionMessage message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        SaturationStatusMessage statusMessage = message.getStatusMessage();
        switch (statusMessage) {
            case CONTROL_NODE_REQUESTS_SATURATION_INTERRUPT:
            case CONTROL_NODE_REQUESTS_SATURATION_STATUS:
                communicationChannel.sendToControlNode(SaturationStatusMessage.PARTITION_INFO_SATURATION_RUNNING);
                break;
            case CONTROL_NODE_REQUESTS_SATURATION_CONTINUATION:
                // do nothing
                break;
            default:
                messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(SaturationAxiomsMessage message) {
        Collection<?> axioms = message.getAxioms();
        for (Object axiom : axioms) {
            incrementalReasoner.processAxiom(axiom);
        }
    }
}

