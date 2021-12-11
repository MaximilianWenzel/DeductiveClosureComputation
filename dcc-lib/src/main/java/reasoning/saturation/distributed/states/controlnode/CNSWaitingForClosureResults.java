package reasoning.saturation.distributed.states.controlnode;

import data.Closure;
import networking.messages.AcknowledgementMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class CNSWaitingForClosureResults<C extends Closure<A>, A extends Serializable, T extends Serializable> extends ControlNodeState<C, A, T> {

    protected int numberOfWorkers;

    public CNSWaitingForClosureResults(SaturationControlNode<C, A, T> saturationControlNode) {
        super(saturationControlNode);
        this.numberOfWorkers = saturationControlNode.getWorkers().size();
    }

    @Override
    public void visit(SaturationAxiomsMessage<C, A, T> message) {
        saturationControlNode.addAxiomsToClosureResult(message.getAxioms());
        communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        // 'closure result' requests of control node are acknowledged if all results have been transmitted
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());

        if (communicationChannel.getReceivedClosureResultsCounter().get() == this.numberOfWorkers) {
            log.info("All closure results received.");
            saturationControlNode.switchState(new CNSFinished<>(saturationControlNode));
        }
    }

    @Override
    public void visit(StateInfoMessage message) {
        // no state messages allowed anymore
        messageProtocolViolation(message);
    }
}
