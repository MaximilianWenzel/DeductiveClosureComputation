package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import enums.StatisticsComponent;
import exceptions.MessageProtocolViolationException;
import networking.messages.*;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;

public class WorkerStateRunning<C extends Closure<A>, A extends Serializable, T extends Serializable> extends WorkerState<C, A, T> {

    public WorkerStateRunning(SaturationWorker<C, A, T> worker) {
        super(worker);
    }

    public void processMessage(Object msg) {
        if (msg instanceof MessageModel) {
            ((MessageModel<C, A, T>) msg).accept(this);
        } else {
            this.visit((A) msg);
        }
    }

    public void onToDoIsEmpty() {
        this.worker.switchState(new WorkerStateConverged<>(worker));
        if (config.collectWorkerNodeStatistics()) {
            stats.getTodoIsEmptyEvent().incrementAndGet();
            stats.startStopwatch(StatisticsComponent.WORKER_WAITING_TIME_SATURATION);
        }
        if (worker.getSentAxiomMessages().get() > 0
            || worker.getReceivedAxiomMessages().get() > 0) {
            worker.sendAxiomCountMessageToControlNode();
        }
    }


    @Override
    public void visit(InitializeWorkerMessage<C, A, T> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case TODO_IS_EMPTY_EVENT:
                onToDoIsEmpty();
                break;
            case WORKER_SERVER_HELLO:
                // do nothing
                break;
            case WORKER_CLIENT_HELLO:
                worker.acknowledgeMessage(message.getSenderID(), message.getMessageID());
                break;
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
        worker.getSaturationStageCounter().set(message.getStage());
        worker.sendAxiomCountMessageToControlNode();
    }

    @Override
    public void visit(A axiom) {
        this.incrementalReasoner.getStreamOfInferencesForGivenAxiom(axiom)
                // only those which are not contained in closure
                .filter(inference -> !worker.getClosure().contains(inference))
                .flatMap(inference -> worker.getAxiomMessagesFromInferenceStream(inference))
                .forEach(worker::sendMessage);
    }
}

