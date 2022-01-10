package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import enums.StatisticsComponent;
import exceptions.MessageProtocolViolationException;
import networking.messages.*;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;

public class WorkerStateRunning<C extends Closure<A>, A extends Serializable, T extends Serializable> extends WorkerState<C, A, T> {

    private boolean lastMessageWasAxiomCountRequest = false;

    public WorkerStateRunning(SaturationWorker<C, A, T> worker) {
        super(worker);
    }

    public void mainWorkerLoop() throws InterruptedException {
        if (!communicationChannel.hasMoreMessages()) {
            this.worker.switchState(new WorkerStateConverged<>(worker));
            if (config.collectWorkerNodeStatistics()) {
                stats.getTodoIsEmptyEvent().incrementAndGet();
                stats.stopStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
                stats.startStopwatch(StatisticsComponent.WORKER_WAITING_TIME_SATURATION);
            }
            if (!lastMessageWasAxiomCountRequest) {
                communicationChannel.sendAxiomCountToControlNode();
            }
            return;
        }

        Object obj = communicationChannel.read();
        if (obj instanceof MessageModel) {
            ((MessageModel<C, A, T>)obj).accept(this);
        } else {
            incrementalReasoner.processAxiom((A) obj);
        }

        if (obj instanceof RequestAxiomMessageCount) {
            lastMessageWasAxiomCountRequest = true;
        } else {
            lastMessageWasAxiomCountRequest = false;
        }
    }


    @Override
    public void visit(InitializeWorkerMessage<C, A, T> message) {
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
        incrementalReasoner.processAxiom(axiom);
    }
}

