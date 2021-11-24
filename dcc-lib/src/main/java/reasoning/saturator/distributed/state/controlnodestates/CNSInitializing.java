package reasoning.saturator.distributed.state.controlnodestates;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturator.distributed.SaturationControlNode;
import reasoning.saturator.distributed.SaturationPartition;

import java.util.Map;
import java.util.Set;

public class CNSInitializing extends ControlNodeState {

    protected Set<SaturationPartition> initializedPartitions;
    protected Map<Long, SaturationPartition> partitionIDToPartition;
    protected int numberOfPartitions;

    public CNSInitializing(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getPartitions().size();
        this.initializedPartitions = new UnifiedSet<>();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_INFO_INITIALIZED:
                SaturationPartition partition = partitionIDToPartition.get(message.getSenderID());
                initializedPartitions.add(partition);
                if (initializedPartitions.size() == numberOfPartitions) {
                    // all partitions initialized
                    saturationControlNode.switchState(new CNSWaitingForPartitionsToConverge(saturationControlNode));
                    communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_REQUESTS_START_SATURATION);
                }
            default:
                throw new MessageProtocolViolationException();
        }
    }
}
