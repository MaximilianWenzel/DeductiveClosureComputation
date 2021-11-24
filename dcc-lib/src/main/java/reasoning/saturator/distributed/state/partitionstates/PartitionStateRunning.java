package reasoning.saturator.distributed.state.partitionstates;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.InitializePartitionMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturator.distributed.SaturationCommunicationChannel;
import reasoning.saturator.distributed.SaturationPartition;

import java.util.Collection;

public class PartitionStateRunning extends PartitionState {

    SaturationCommunicationChannel communicationChannel;

    public PartitionStateRunning(SaturationPartition partition) {
        super(partition);
        this.communicationChannel = partition.getCommunicationChannel();
    }

    @Override
    public void onToDoQueueIsEmpty() {
        partition.switchState(new PartitionStateConverged(partition));
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
                communicationChannel.sendToControlNode(SaturationStatusMessage.PARTITION_INFO_SATURATION_RUNNING);
                break;
            case CONTROL_NODE_REQUESTS_SATURATION_CONTINUATION:
                // do nothing
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void visit(SaturationAxiomsMessage message) {
        Collection<Object> axioms = message.getAxioms();
        for (Object axiom : axioms) {
            partition.processAxiom(axiom);
        }
    }
}

