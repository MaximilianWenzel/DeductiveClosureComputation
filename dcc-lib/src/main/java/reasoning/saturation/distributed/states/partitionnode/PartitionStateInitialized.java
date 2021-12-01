package reasoning.saturation.distributed.states.partitionnode;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.InitializePartitionMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationPartition;

import java.util.ArrayList;
import java.util.List;

public class PartitionStateInitialized extends PartitionState {

    /**
     * Axioms which have been received from other partitions while in state initialized (since these partitions
     * have received the start signal from control node sooner).
     */
    private List<SaturationAxiomsMessage> bufferedMessages = new ArrayList<>();

    public PartitionStateInitialized(SaturationPartition partition) {
        super(partition);
    }

    @Override
    public void visit(InitializePartitionMessage message) {
        throw new IllegalArgumentException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        SaturationStatusMessage status = message.getStatusMessage();
        if (status == SaturationStatusMessage.CONTROL_NODE_REQUESTS_START_SATURATION) {
            log.info("Start signal received from control node.");
            communicationChannel.addInitialAxiomsToQueue();
            communicationChannel.addAxiomsToQueue(bufferedMessages);
            partition.switchState(new PartitionStateRunning(partition));
        } else {
            messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(SaturationAxiomsMessage message) {
        this.bufferedMessages.add(message);
    }
}
