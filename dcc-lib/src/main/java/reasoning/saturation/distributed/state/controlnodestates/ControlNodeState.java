package reasoning.saturation.distributed.state.controlnodestates;

import exceptions.MessageProtocolViolationException;
import networking.messages.DebugMessage;
import networking.messages.InitializePartitionMessage;
import networking.messages.MessageModelVisitor;
import networking.messages.SaturationAxiomsMessage;
import reasoning.saturation.distributed.SaturationControlNode;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
import util.ConsoleUtils;

import java.util.logging.Logger;

public abstract class ControlNodeState implements MessageModelVisitor {

    protected final Logger log = ConsoleUtils.getLogger();

    protected ControlNodeCommunicationChannel communicationChannel;
    protected SaturationControlNode saturationControlNode;

    public ControlNodeState(SaturationControlNode saturationControlNode) {
        this.saturationControlNode = saturationControlNode;
        this.communicationChannel = saturationControlNode.getCommunicationChannel();
    }

    @Override
    public void visit(InitializePartitionMessage message) {
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
}
