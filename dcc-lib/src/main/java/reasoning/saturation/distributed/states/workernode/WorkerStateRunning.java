package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import enums.StatisticsComponent;
import exceptions.MessageProtocolViolationException;
import networking.messages.*;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;

public class WorkerStateRunning<C extends Closure<A>, A extends Serializable> extends WorkerState<C, A> {

    private boolean lastMessageWasAxiomCountRequest = false;

    public WorkerStateRunning(SaturationWorker<C, A> worker) {
        super(worker);
    }

    public void mainWorkerLoop(Object obj) {
        if (obj instanceof MessageModel) {
            ((MessageModel<C, A>) obj).accept(this);
        } else {
            visit((A) obj);
        }

        lastMessageWasAxiomCountRequest = obj instanceof RequestAxiomMessageCount;
    }

    public void onToDoIsEmpty() {
        this.worker.switchState(new WorkerStateConverged<>(worker));
        if (config.collectWorkerNodeStatistics()) {
            stats.getTodoIsEmptyEvent().incrementAndGet();
            stats.startStopwatch(StatisticsComponent.WORKER_WAITING_TIME_SATURATION);
        }
        if (!lastMessageWasAxiomCountRequest) {
            communicationChannel.sendAxiomCountToControlNode();
        }
    }


    @Override
    public void visit(InitializeWorkerMessage<C, A> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case WORKER_SERVER_HELLO:
            case WORKER_CLIENT_HELLO:
                communicationChannel.acknowledgeMessage(message.getSenderID(), message.getMessageID());
                break;
            case CONTROL_NODE_REQUEST_SEND_CLOSURE_RESULT:

            default:
                messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());
    }

    @Override
    public void visit(RequestAxiomMessageCount message) {
        communicationChannel.getSaturationStageCounter().set(message.getStage());
        communicationChannel.sendAxiomCountToControlNode();
    }

    @Override
    public void visit(A axiom) {
        communicationChannel.distributeInferences(incrementalReasoner.getStreamOfInferencesForGivenAxiom(axiom));
    }

}

