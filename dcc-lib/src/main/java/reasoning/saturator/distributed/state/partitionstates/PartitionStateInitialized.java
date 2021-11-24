package reasoning.saturator.distributed.state.partitionstates;

import enums.SaturationStatusMessage;
import exceptions.IllegalEventException;
import networking.messages.InitializePartitionMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturator.distributed.SaturationPartition;

public class PartitionStateInitialized extends PartitionState {

    public PartitionStateInitialized(SaturationPartition partition) {
        super(partition);
        partition.getCommunicationChannel().sendToControlNode(SaturationStatusMessage.PARTITION_INFO_INITIALIZED);
    }

    @Override
    public void onToDoQueueIsEmpty() {
        throw new IllegalEventException("ToDo queue should not be accessed yet.");
    }

    @Override
    public void visit(InitializePartitionMessage message) {
        throw new IllegalArgumentException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        SaturationStatusMessage status = message.getStatusMessage();
        if (status == SaturationStatusMessage.CONTROL_NODE_REQUESTS_START_SATURATION) {
            partition.switchState(new PartitionStateRunning(partition));
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visit(SaturationAxiomsMessage message) {
        throw new IllegalArgumentException();
    }
}
