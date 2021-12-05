package reasoning.saturation.distributed.states.controlnode;

import data.Closure;
import exceptions.MessageProtocolViolationException;
import networking.messages.AcknowledgementMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

import java.io.Serializable;

public class CNSFinished<C extends Closure<A>, A extends Serializable> extends ControlNodeState<C, A> {
    public CNSFinished(SaturationControlNode<C, A> saturationControlNode) {
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
