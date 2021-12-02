package reasoning.saturation.distributed.states.controlnode;

import exceptions.MessageProtocolViolationException;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.messages.*;
import reasoning.saturation.distributed.SaturationControlNode;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
import util.ConsoleUtils;

import java.util.logging.Logger;

public abstract class ControlNodeState implements MessageModelVisitor {

    protected final Logger log = ConsoleUtils.getLogger();

    protected ControlNodeCommunicationChannel communicationChannel;
    protected SaturationControlNode saturationControlNode;
    protected AcknowledgementEventManager acknowledgementEventManager;

    public ControlNodeState(SaturationControlNode saturationControlNode) {
        this.saturationControlNode = saturationControlNode;
        this.communicationChannel = saturationControlNode.getCommunicationChannel();
        this.acknowledgementEventManager = communicationChannel.getAcknowledgementEventManager();
    }

    public void mainControlNodeLoop() throws InterruptedException {
        MessageModel message = (MessageModel) communicationChannel.read();
        message.accept(this);
    }

    @Override
    public void visit(InitializeWorkerMessage message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(DebugMessage message) {
        log.info(message.getMessage());
    }

    @Override
    public void visit(SaturationAxiomsMessage message) {
        // only allowed if closure has been requested by control node
        throw new MessageProtocolViolationException();
    }

    protected void messageProtocolViolation(StateInfoMessage message) {
        log.warning("State: " + this.getClass() + ", message type: " + message.getStatusMessage());
        throw new MessageProtocolViolationException();
    }
}
