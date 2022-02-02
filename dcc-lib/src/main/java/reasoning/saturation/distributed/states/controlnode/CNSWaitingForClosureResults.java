package reasoning.saturation.distributed.states.controlnode;

import data.Closure;
import enums.SaturationStatusMessage;
import enums.StatisticsComponent;
import networking.messages.AcknowledgementMessage;
import networking.messages.StateInfoMessage;
import networking.messages.StatisticsMessage;
import reasoning.saturation.distributed.SaturationControlNode;

import java.io.Serializable;

public class CNSWaitingForClosureResults<C extends Closure<A>, A extends Serializable, T extends Serializable>
        extends ControlNodeState<C, A, T> {

    protected int numberOfWorkers;

    public CNSWaitingForClosureResults(SaturationControlNode<C, A, T> saturationControlNode) {
        super(saturationControlNode);
        this.numberOfWorkers = saturationControlNode.getWorkers().size();
    }

    @Override
    public void visit(A axiom) {
        saturationControlNode.addAxiomToClosureResult(axiom);
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case TODO_IS_EMPTY_EVENT:
                // ignore
                break;
            default:
                messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        // 'closure result' requests of control node are acknowledged if all results have been transmitted
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());
    }

    @Override
    public void visit(StatisticsMessage message) {
        saturationControlNode.getWorkerStatistics().add(message.getStatistics());
    }
}
