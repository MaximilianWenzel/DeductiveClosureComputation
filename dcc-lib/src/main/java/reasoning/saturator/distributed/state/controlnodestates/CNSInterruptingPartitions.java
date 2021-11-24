package reasoning.saturator.distributed.state.controlnodestates;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturator.distributed.SaturationControlNode;
import reasoning.saturator.distributed.SaturationPartition;

import java.util.Set;

public class CNSInterruptingPartitions extends ControlNodeState {
    protected Set<SaturationPartition> interruptedPartitions = new UnifiedSet<>();
    protected int numberOfPartitions;

    public CNSInterruptingPartitions(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getPartitions().size();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_INFO_SATURATION_INTERRUPTED:
                interruptedPartitions.add(saturationControlNode.getPartition(message.getSenderID()));
                if (interruptedPartitions.size() == numberOfPartitions) {
                    saturationControlNode.switchState(new CNSCheckPartitionSaturationStatus(saturationControlNode));
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
