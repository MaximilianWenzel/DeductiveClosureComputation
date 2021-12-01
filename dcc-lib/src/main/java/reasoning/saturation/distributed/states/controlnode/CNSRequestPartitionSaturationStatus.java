package reasoning.saturation.distributed.states.controlnode;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.distributed.SaturationControlNode;
import reasoning.saturation.models.PartitionModel;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class CNSRequestPartitionSaturationStatus extends ControlNodeState {
    protected Set<Long> convergedPartitions = new UnifiedSet<>();
    protected int numberOfPartitions;

    public CNSRequestPartitionSaturationStatus(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getPartitions().size();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_INFO_SATURATION_CONVERGED:
                convergedPartitions.add(message.getSenderID());
                log.info("Partition " + message.getSenderID() + " has status 'converged'.");
                if (convergedPartitions.size() == numberOfPartitions) {
                    log.info("All partition have status 'converged'.");
                    log.info("Waiting for closure results...");
                    saturationControlNode.switchState(new CNSWaitingForClosureResults(saturationControlNode));
                    communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_INFO_ALL_PARTITIONS_CONVERGED);
                }
                break;
            case PARTITION_INFO_SATURATION_RUNNING:
                log.info("Partition " + message.getSenderID() + " has status 'running'.");
                log.info("Requesting continuation of distributed saturation procedure...");
                saturationControlNode.switchState(new CNSWaitingForPartitionsToConverge(saturationControlNode));
                communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_REQUESTS_SATURATION_CONTINUATION);
                break;
            default:
                messageProtocolViolation(message);
        }
    }
}
