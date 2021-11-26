package reasoning.saturation.distributed.state.controlnodestates;

import exceptions.MessageProtocolViolationException;
import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

import java.util.concurrent.atomic.AtomicInteger;

public class CNSWaitingForClosureResults extends ControlNodeState {

    protected AtomicInteger receivedClosureResults = new AtomicInteger(0);
    protected int numberOfPartitions;

    public CNSWaitingForClosureResults(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getPartitions().size();
    }

    @Override
    public void visit(SaturationAxiomsMessage message) {
        // TODO what if multiple messages required to send complete closure?
        receivedClosureResults.getAndIncrement();
        saturationControlNode.addAxiomsToClosureResult(message.getAxioms());
        if (receivedClosureResults.get() == numberOfPartitions) {
            saturationControlNode.switchState(new CNSFinished(saturationControlNode));
        }
    }

    @Override
    public void visit(StateInfoMessage message) {
        // no state messages allowed anymore
        throw new MessageProtocolViolationException();
    }
}
