package reasoning.saturation.distributed.states.controlnode;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.distributed.SaturationControlNode;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class CNSInterruptingPartitions extends ControlNodeState {
    protected Set<Long> interruptedPartitions = new UnifiedSet<>();
    protected int numberOfPartitions;

    public CNSInterruptingPartitions(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getPartitions().size();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_INFO_SATURATION_INTERRUPTED:
                interruptedPartitions.add(message.getSenderID());
                log.info("Partition " + message.getSenderID() + " interrupted.");
                log.info("(" + interruptedPartitions.size() + "/" + numberOfPartitions + ") partitions interrupted.");
                if (interruptedPartitions.size() == numberOfPartitions) {
                    log.info("All partitions have been interrupted.");
                    log.info("Requesting partition states...");
                    saturationControlNode.switchState(new CNSRequestPartitionSaturationStatus(saturationControlNode));
                    communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_REQUESTS_SATURATION_STATUS);
                }
                break;
            case PARTITION_INFO_SATURATION_CONVERGED:
                // do nothing
                break;
            case PARTITION_INFO_SATURATION_RUNNING:
                saturationControlNode.switchState(new CNSWaitingForPartitionsToConverge(saturationControlNode));
                communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_REQUESTS_SATURATION_CONTINUATION);
                break;
            default:
                messageProtocolViolation(message);
        }
    }
}
