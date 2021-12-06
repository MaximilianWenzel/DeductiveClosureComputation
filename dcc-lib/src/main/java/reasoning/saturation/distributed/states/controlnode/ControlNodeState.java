package reasoning.saturation.distributed.states.controlnode;

import data.Closure;
import exceptions.MessageProtocolViolationException;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.messages.*;
import reasoning.saturation.distributed.SaturationControlNode;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
import util.ConsoleUtils;

import java.io.Serializable;
import java.util.logging.Logger;

public abstract class ControlNodeState<C extends Closure<A>, A extends Serializable, T extends Serializable> implements MessageModelVisitor<C, A, T> {

    protected final Logger log = ConsoleUtils.getLogger();

    protected ControlNodeCommunicationChannel<C, A, T> communicationChannel;
    protected SaturationControlNode<C, A, T> saturationControlNode;
    protected AcknowledgementEventManager acknowledgementEventManager;

    public ControlNodeState(SaturationControlNode<C, A, T> saturationControlNode) {
        this.saturationControlNode = saturationControlNode;
        this.communicationChannel = saturationControlNode.getCommunicationChannel();
        this.acknowledgementEventManager = communicationChannel.getAcknowledgementEventManager();
    }

    public void mainControlNodeLoop() throws InterruptedException {
        MessageModel<C, A, T> message = (MessageModel<C, A, T>) communicationChannel.read();
        message.accept(this);
    }

    @Override
    public void visit(InitializeWorkerMessage<C, A, T> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(DebugMessage message) {
        log.info(message.getMessage());
    }

    @Override
    public void visit(SaturationAxiomsMessage<C, A, T> message) {
        // only allowed if closure has been requested by control node
        throw new MessageProtocolViolationException();
    }

    protected void messageProtocolViolation(StateInfoMessage message) {
        log.warning("State: " + this.getClass() + ", message type: " + message.getStatusMessage());
        throw new MessageProtocolViolationException();
    }
}
