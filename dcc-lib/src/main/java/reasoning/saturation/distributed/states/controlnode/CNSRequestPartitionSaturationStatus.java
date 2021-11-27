package reasoning.saturation.distributed.states.controlnode;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

import java.util.concurrent.atomic.AtomicInteger;

public class CNSRequestPartitionSaturationStatus extends ControlNodeState {
    protected AtomicInteger convergedPartitions = new AtomicInteger(0);
    protected int numberOfPartitions;

    public CNSRequestPartitionSaturationStatus(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getPartitions().size();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_INFO_SATURATION_CONVERGED:
                convergedPartitions.incrementAndGet();
                if (convergedPartitions.get() == numberOfPartitions) {
                    saturationControlNode.switchState(new CNSWaitingForPartitionsToConverge(saturationControlNode));
                    communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_INFO_ALL_PARTITIONS_CONVERGED);
                }
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
