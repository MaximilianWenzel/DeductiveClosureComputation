package reasoning.saturation.distributed.states.controlnode;

import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

public class CNSFinished extends ControlNodeState {
    public CNSFinished(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
    }

    @Override
    public void visit(StateInfoMessage message) {
        messageProtocolViolation(message);
    }
}
