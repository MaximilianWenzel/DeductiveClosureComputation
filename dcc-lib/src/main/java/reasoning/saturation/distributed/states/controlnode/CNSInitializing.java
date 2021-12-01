package reasoning.saturation.distributed.states.controlnode;

import enums.SaturationStatusMessage;
import networking.messages.StateInfoMessage;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.distributed.SaturationControlNode;

import java.util.Set;

public class CNSInitializing extends ControlNodeState {

    protected Set<Long> initializedPartitions = new UnifiedSet<>();
    protected int numberOfPartitions;

    public CNSInitializing(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getPartitions().size();
    }


    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_SERVER_HELLO:
                // do nothing
                break;
            case PARTITION_INFO_INITIALIZED:
                initializedPartitions.add(message.getSenderID());
                if (initializedPartitions.size() == numberOfPartitions) {
                    log.info("All partitions successfully initialized. Sending start signal to all partitions.");
                    // all partitions initialized
                    saturationControlNode.switchState(new CNSWaitingForPartitionsToConverge(saturationControlNode));
                    communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_REQUESTS_START_SATURATION);
                }
                break;
            default:
                messageProtocolViolation(message);
        }
    }
}
