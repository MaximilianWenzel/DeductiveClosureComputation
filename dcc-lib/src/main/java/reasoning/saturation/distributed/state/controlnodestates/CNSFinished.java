package reasoning.saturation.distributed.state.controlnodestates;

import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

public class CNSFinished extends ControlNodeState {
    public CNSFinished(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
    }

    @Override
    public void visit(StateInfoMessage message) {
        throw new MessageProtocolViolationException();
    }
}
