package reasoning.saturation.distributed.states.controlnode;

import data.Closure;
import exceptions.MessageProtocolViolationException;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.messages.*;
import reasoning.saturation.distributed.SaturationControlNode;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.distributed.states.AxiomVisitor;
import util.ConsoleUtils;

import java.io.Serializable;
import java.util.logging.Logger;

public abstract class ControlNodeState<C extends Closure<A>, A extends Serializable, T extends Serializable> implements MessageModelVisitor<C, A, T>,
        AxiomVisitor<A> {

    protected final Logger log = ConsoleUtils.getLogger();

    protected ControlNodeCommunicationChannel<C, A, T> communicationChannel;
    protected SaturationControlNode<C, A, T> saturationControlNode;
    protected AcknowledgementEventManager acknowledgementEventManager;
    protected SaturationConfiguration config;
    protected ControlNodeStatistics stats;

    public ControlNodeState(SaturationControlNode<C, A, T> saturationControlNode) {
        this.saturationControlNode = saturationControlNode;
        this.communicationChannel = saturationControlNode.getCommunicationChannel();
        this.acknowledgementEventManager = communicationChannel.getAcknowledgementEventManager();
        this.config = saturationControlNode.getConfig();
        this.stats = saturationControlNode.getControlNodeStatistics();
    }

    public void mainControlNodeLoop() throws InterruptedException {
        Object msg = communicationChannel.read();
        if (msg instanceof MessageModel) {
            MessageModel<C, A, T> message = (MessageModel<C, A, T>) msg;
            message.accept(this);
        } else {
            visit((A) msg);
        }
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
    public void visit(AxiomCount message) {
        System.err.println(message);
        throw new MessageProtocolViolationException("Axiom count message received in state " + this.getClass());
    }

    @Override
    public void visit(StateInfoMessage message) {
        messageProtocolViolation(message);
    }

    @Override
    public void visit(RequestAxiomMessageCount message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StatisticsMessage message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(A axiom) {
        throw new MessageProtocolViolationException("Axioms only allowed when Control Node waits for closure results from workers.");
    }

    protected void messageProtocolViolation(StateInfoMessage message) {
        log.warning("State: " + this.getClass() + ", message type: " + message.getStatusMessage());
        throw new MessageProtocolViolationException();
    }
}
