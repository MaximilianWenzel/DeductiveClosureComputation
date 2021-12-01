package reasoning.saturation.distributed.states.controlnode;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.distributed.SaturationControlNode;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class CNSWaitingForPartitionsToConverge extends ControlNodeState {

    protected Set<Long> convergedPartitions = new UnifiedSet<>();
    protected int numberOfPartitions;

    public CNSWaitingForPartitionsToConverge(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getPartitions().size();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_INFO_SATURATION_CONVERGED:
                log.info("Partition " + message.getSenderID() + " converged.");
                convergedPartitions.add(message.getSenderID());
                log.info("(" + convergedPartitions.size() + "/" + numberOfPartitions + ") partitions converged.");
                if (convergedPartitions.size() == numberOfPartitions) {
                    log.info("All partitions converged.");
                    log.info("Attempt to interrupt all partitions...");
                    saturationControlNode.switchState(new CNSInterruptingPartitions(saturationControlNode));
                    communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_REQUESTS_SATURATION_INTERRUPT);
                }
                break;
            case PARTITION_INFO_SATURATION_RUNNING:
                log.info("Partition " + message.getSenderID() + " is running again.");
                convergedPartitions.remove(message.getSenderID());
                break;
            case PARTITION_INFO_SATURATION_INTERRUPTED:
                // ignore
                break;
            default:
                messageProtocolViolation(message);
        }

    }

}
