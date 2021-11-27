package reasoning.saturation.distributed.states.partitionnode;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.InitializePartitionMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationPartition;

public class PartitionStateInterrupted extends PartitionState {

    public PartitionStateInterrupted(SaturationPartition partition) {
        super(partition);
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
                communicationChannel.sendToControlNode(SaturationStatusMessage.PARTITION_INFO_SATURATION_INTERRUPTED);
                break;
            case CONTROL_NODE_REQUESTS_SATURATION_CONTINUATION:
                partition.switchState(new PartitionStateConverged(partition));
                break;
            case CONTROL_NODE_REQUESTS_SATURATION_STATUS:
                if (communicationChannel.hasMoreMessages()) {
                    // TODO crucial state transition here: maybe optimization required
                    partition.switchState(new PartitionStateRunning(partition));
                    communicationChannel.sendToControlNode(SaturationStatusMessage.PARTITION_INFO_SATURATION_RUNNING);
                } else {
                    communicationChannel.sendToControlNode(SaturationStatusMessage.PARTITION_INFO_SATURATION_CONVERGED);
                }
                break;
            case CONTROL_NODE_INFO_ALL_PARTITIONS_CONVERGED:
                communicationChannel.sendToControlNode(partition.getClosure());
                partition.switchState(new PartitionStateFinished(partition));
                break;
            default:
                throw new MessageProtocolViolationException();
        }
    }

    @Override
    public void visit(SaturationAxiomsMessage message) {
        throw new MessageProtocolViolationException();
    }
}
