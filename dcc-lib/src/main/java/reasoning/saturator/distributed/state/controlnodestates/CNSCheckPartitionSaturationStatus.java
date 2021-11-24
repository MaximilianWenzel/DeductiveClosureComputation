package reasoning.saturator.distributed.state.controlnodestates;

import networking.messages.StateInfoMessage;
import reasoning.saturator.distributed.SaturationControlNode;

public class CNSCheckPartitionSaturationStatus extends ControlNodeState {
    public CNSCheckPartitionSaturationStatus(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_INFO_SATURATION_CONVERGED:
                // TODO
        }
    }
}
