package reasoning.saturation.distributed.states.controlnode;

import data.Closure;
import exceptions.MessageProtocolViolationException;
import networking.messages.AcknowledgementMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

import java.io.Serializable;

public class CNSFinished<C extends Closure<A>, A extends Serializable, T extends Serializable> extends ControlNodeState<C, A, T> {
    public CNSFinished(SaturationControlNode<C, A, T> saturationControlNode) {
        super(saturationControlNode);
    }

    @Override
    public void visit(StateInfoMessage message) {
        messageProtocolViolation(message);
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        throw new MessageProtocolViolationException();
    }
}
