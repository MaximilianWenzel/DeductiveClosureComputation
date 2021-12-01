package reasoning.saturation.distributed.states.controlnode;

import networking.messages.SaturationAxiomsMessage;
import networking.messages.StateInfoMessage;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.distributed.SaturationControlNode;

import java.util.Set;

public class CNSWaitingForClosureResults extends ControlNodeState {

    protected Set<Long> receivedClosureResults = new UnifiedSet<>();
    protected int numberOfPartitions;

    public CNSWaitingForClosureResults(SaturationControlNode saturationControlNode) {
        super(saturationControlNode);
        this.numberOfPartitions = saturationControlNode.getPartitions().size();
    }

    @Override
    public void visit(SaturationAxiomsMessage message) {
        // TODO what if multiple messages required to send complete closure?
        receivedClosureResults.add(message.getSenderID());
        saturationControlNode.addAxiomsToClosureResult(message.getAxioms());
        log.info("Partition " + message.getSenderID() + " sent closure results.");
        log.info("(" + receivedClosureResults.size() + "/" + numberOfPartitions + ") closure results received.");
        if (receivedClosureResults.size() == numberOfPartitions) {
            log.info("All closure results received.");
            saturationControlNode.switchState(new CNSFinished(saturationControlNode));
        }
    }

    @Override
    public void visit(StateInfoMessage message) {
        // no state messages allowed anymore
        messageProtocolViolation(message);
    }
}
