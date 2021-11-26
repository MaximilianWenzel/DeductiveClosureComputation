package reasoning.saturation.distributed.state.controlnodestates;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

import java.util.concurrent.atomic.AtomicInteger;

public class CNSInterruptingPartitions extends ControlNodeState {
    protected AtomicInteger interruptedPartitions = new AtomicInteger(0);
    protected int numberOfPartitions;

    public CNSInterruptingPartitions(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getPartitions().size();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_INFO_SATURATION_INTERRUPTED:
                interruptedPartitions.getAndIncrement();
                if (interruptedPartitions.get() == numberOfPartitions) {
                    saturationControlNode.switchState(new CNSRequestPartitionSaturationStatus(saturationControlNode));
                    communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_REQUESTS_SATURATION_STATUS);
                }
                break;
            case PARTITION_INFO_TODO_IS_EMPTY:
                // do nothing
                break;
            case PARTITION_INFO_SATURATION_RUNNING:
                saturationControlNode.switchState(new CNSWaitingForPartitionsToConverge(saturationControlNode));
                communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_REQUESTS_SATURATION_CONTINUATION);
                break;
            default:
                throw new MessageProtocolViolationException();
        }
    }
}
