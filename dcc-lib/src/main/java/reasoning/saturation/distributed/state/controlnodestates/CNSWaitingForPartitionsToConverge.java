package reasoning.saturation.distributed.state.controlnodestates;

import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

import java.util.concurrent.atomic.AtomicInteger;

public class CNSWaitingForPartitionsToConverge extends ControlNodeState {

    protected AtomicInteger convergedPartitions = new AtomicInteger(0);
    protected int numberOfPartitions;

    public CNSWaitingForPartitionsToConverge(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getPartitions().size();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_INFO_SATURATION_CONVERGED:
                convergedPartitions.getAndIncrement();
                if (convergedPartitions.get() == numberOfPartitions) {
                    saturationControlNode.switchState(new CNSInterruptingPartitions(saturationControlNode));
                }
                break;
            case PARTITION_INFO_TODO_IS_EMPTY:
                convergedPartitions.getAndIncrement();
                break;
            case PARTITION_INFO_SATURATION_RUNNING:
                convergedPartitions.getAndDecrement();
                break;
            default:
                throw new MessageProtocolViolationException();
        }

    }

}
