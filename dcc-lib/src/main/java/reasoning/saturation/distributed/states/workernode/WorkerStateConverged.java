package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import enums.SaturationStatusMessage;
import enums.StatisticsComponent;
import exceptions.MessageProtocolViolationException;
import networking.messages.AcknowledgementMessage;
import networking.messages.InitializeWorkerMessage;
import networking.messages.RequestAxiomMessageCount;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationWorker;

import java.io.Serializable;

public class WorkerStateConverged<C extends Closure<A>, A extends Serializable, T extends Serializable>
        extends WorkerState<C, A, T> {


    public WorkerStateConverged(SaturationWorker<C, A, T> worker) {
        super(worker);
    }

    @Override
    public void visit(InitializeWorkerMessage<C, A, T> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        SaturationStatusMessage statusMessage = message.getStatusMessage();
        switch (statusMessage) {
            case CONTROL_NODE_REQUEST_SEND_CLOSURE_RESULT:
                if (config.collectWorkerNodeStatistics()) {
                    // finalize statistics
                    stats.stopStopwatch(StatisticsComponent.WORKER_WAITING_TIME_SATURATION);
                    stats.collectStopwatchTimes();
                    communicationChannel.sendToControlNode(stats);
                }
                log.info("Control node requests closure results...");
                communicationChannel.sendToControlNode(worker.getClosure());
                long sendClosureResultRequestMessageID = message.getMessageID();
                communicationChannel.acknowledgeMessage(communicationChannel.getControlNodeID(),
                        sendClosureResultRequestMessageID);
                break;
            case WORKER_SERVER_HELLO:
            case WORKER_CLIENT_HELLO:
                acknowledgementEventManager.messageAcknowledged(message.getMessageID());
                break;
            case CONTROL_NODE_INFO_CLOSURE_RESULTS_RECEIVED:
                log.info("Control node received all closure results. Saturation finished.");
                worker.switchState(new WorkerStateFinished<>(worker));
                break;
            default:
                messageProtocolViolation(message);
        }
    }

    @Override
    public void visit(A axiom) {
        WorkerStateRunning<C, A, T> runningState = new WorkerStateRunning<>(worker);
        //log.info("Axioms received. Continuing saturation...");
        if (config.collectWorkerNodeStatistics()) {
            stats.startStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
            stats.stopStopwatch(StatisticsComponent.WORKER_WAITING_TIME_SATURATION);
        }
        worker.switchState(runningState);
        runningState.visit(axiom);
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        communicationChannel.getAcknowledgementEventManager().messageAcknowledged(message.getMessageID());
    }

    @Override
    public void visit(RequestAxiomMessageCount message) {
        communicationChannel.getSaturationStageCounter().set(message.getStage());
        communicationChannel.sendAxiomCountToControlNode();
    }
}
