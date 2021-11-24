package reasoning.saturator.distributed.state.controlnodestates;

import networking.messages.StateInfoMessage;
import reasoning.saturator.distributed.SaturationControlNode;

public class CNSFinished extends ControlNodeState {
    public CNSFinished(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
    }

    @Override
    public void visit(StateInfoMessage message) {

    }
}
