package reasoning.saturation.distributed.states.partitionnode;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.InitializePartitionMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationPartition;

public class PartitionStateConverged extends PartitionState {

    public PartitionStateConverged(SaturationPartition partition) {
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
                log.info("Partition interrupted by control node.");
                communicationChannel.sendToControlNode(SaturationStatusMessage.PARTITION_INFO_SATURATION_INTERRUPTED);
                partition.switchState(new PartitionStateInterrupted(partition));
                break;
            case CONTROL_NODE_REQUESTS_SATURATION_STATUS:
                // ignore
            case CONTROL_NODE_INFO_ALL_PARTITIONS_CONVERGED:
                // ignore
                break;
            case CONTROL_NODE_REQUESTS_SATURATION_CONTINUATION:
                communicationChannel.sendToControlNode(SaturationStatusMessage.PARTITION_INFO_SATURATION_CONVERGED);
                break;
            default:
                messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(SaturationAxiomsMessage message) {
        PartitionStateRunning runningState = new PartitionStateRunning(partition);
        log.info("Axioms received. Continuing saturation...");
        partition.switchState(runningState);
        communicationChannel.sendToControlNode(SaturationStatusMessage.PARTITION_INFO_SATURATION_RUNNING);
        runningState.visit(message);
    }

}
