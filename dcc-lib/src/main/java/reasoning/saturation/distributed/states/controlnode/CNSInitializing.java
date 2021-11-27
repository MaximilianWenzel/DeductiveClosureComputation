package reasoning.saturation.distributed.states.controlnode;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;
import reasoning.saturation.distributed.SaturationPartition;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CNSInitializing extends ControlNodeState {

    protected AtomicInteger initializedPartitions = new AtomicInteger(0);
    protected Map<Long, SaturationPartition> partitionIDToPartition;
    protected int numberOfPartitions;

    public CNSInitializing(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getPartitions().size();
    }


    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_INFO_INITIALIZED:
                SaturationPartition partition = partitionIDToPartition.get(message.getSenderID());
                initializedPartitions.getAndIncrement();
                if (initializedPartitions.get() == numberOfPartitions) {
                    // all partitions initialized
                    saturationControlNode.switchState(new CNSWaitingForPartitionsToConverge(saturationControlNode));
                    communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_REQUESTS_START_SATURATION);
                }
            default:
                throw new MessageProtocolViolationException();
        }
    }
}
