package reasoning.saturation.distributed.states.controlnode;

import data.Closure;
import networking.messages.AcknowledgementMessage;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class CNSWaitingForClosureResults<C extends Closure<A>, A extends Serializable> extends ControlNodeState<C, A> {

    protected int numberOfWorkers;
    protected AtomicInteger receivedClosureResults = new AtomicInteger(0);

    public CNSWaitingForClosureResults(SaturationControlNode<C, A> saturationControlNode) {
        super(saturationControlNode);
        this.numberOfWorkers = saturationControlNode.getWorkers().size();
    }

    @Override
    public void visit(SaturationAxiomsMessage<C, A> message) {
        saturationControlNode.addAxiomsToClosureResult(message.getAxioms());
        log.info("Worker " + message.getSenderID() + " sent closure results.");

    }

    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());
        receivedClosureResults.getAndIncrement();

        if (receivedClosureResults.get() == this.numberOfWorkers) {
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
