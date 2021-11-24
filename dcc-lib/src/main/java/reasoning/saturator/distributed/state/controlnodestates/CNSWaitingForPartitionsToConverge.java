package reasoning.saturator.distributed.state.controlnodestates;

import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturator.distributed.SaturationControlNode;
import reasoning.saturator.distributed.SaturationPartition;

import java.util.Set;

public class CNSWaitingForPartitionsToConverge extends ControlNodeState {

    protected Set<SaturationPartition> convergedPartitions = new UnifiedSet<>();
    protected int numberOfPartitions;

    public CNSWaitingForPartitionsToConverge(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getPartitions().size();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_INFO_SATURATION_CONVERGED:
                convergedPartitions.add(saturationControlNode.getPartition(message.getSenderID()));
                if (convergedPartitions.size() == numberOfPartitions) {
                    saturationControlNode.switchState(new CNSInterruptingPartitions(saturationControlNode));
                }
                break;
            case PARTITION_INFO_TODO_IS_EMPTY:
                convergedPartitions.add(saturationControlNode.getPartition(message.getSenderID()));
                break;
            case PARTITION_INFO_SATURATION_RUNNING:
                convergedPartitions.remove(saturationControlNode.getPartition(message.getSenderID()));
                break;
            default:
                throw new MessageProtocolViolationException();
        }

    }

}
