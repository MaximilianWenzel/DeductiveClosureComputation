package reasoning.saturation.distributed.states.controlnode;

import data.Closure;
import enums.SaturationStatusMessage;
import enums.StatisticsComponent;
import exceptions.MessageProtocolViolationException;
import networking.messages.AcknowledgementMessage;
import networking.messages.AxiomCount;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationControlNode;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class CNSWaitingForWorkersToConverge<C extends Closure<A>, A extends Serializable, T extends Serializable>
        extends ControlNodeState<C, A, T> {

    protected AtomicInteger convergedWorkers = new AtomicInteger();
    protected int numberOfWorkers;
    boolean saturationConvergedVerificationStage = false;

    public CNSWaitingForWorkersToConverge(SaturationControlNode<C, A, T> saturationControlNode) {
        super(saturationControlNode);
        this.numberOfWorkers = saturationControlNode.getWorkers().size();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case WORKER_SERVER_HELLO:
                // ignore
                break;
            default:
                throw new MessageProtocolViolationException("Received message: " + message.getStatusMessage());
        }
    }

    @Override
    public void visit(AcknowledgementMessage message) {
        acknowledgementEventManager.messageAcknowledged(message.getAcknowledgedMessageID());
    }

    @Override
    public void visit(AxiomCount message) {
        if (config.collectControlNodeStatistics()) {
            stats.getNumberOfReceivedAxiomCountMessages().getAndIncrement();
        }

        boolean messageFromLatestSaturationStage = message.getStage() == communicationChannel.getSaturationStage().get();

        AtomicInteger sumOfAllReceivedAxioms = communicationChannel.getSumOfAllReceivedAxioms();
        AtomicInteger sumOfAllSentAxioms = communicationChannel.getSumOfAllSentAxioms();

        sumOfAllReceivedAxioms.addAndGet(message.getReceivedAxioms());
        sumOfAllSentAxioms.addAndGet(message.getSentAxioms());

        if (!messageFromLatestSaturationStage) {
            return;
        }

        if (saturationConvergedVerificationStage) {
            if (message.getReceivedAxioms() == 0 || message.getSentAxioms() == 0) {
                convergedWorkers.getAndIncrement();
                log.info("Worker " + message.getSenderID() + " converged (" + convergedWorkers.get() + "/" + numberOfWorkers + ")");
            } else if (message.getReceivedAxioms() > 0 || message.getSentAxioms() > 0) {
                log.info("Worker " + message.getSenderID() + " is running again.");
                saturationConvergedVerificationStage = false;
                convergedWorkers.set(0);
            }
        }

        if (sumOfAllReceivedAxioms.get() == sumOfAllSentAxioms.get()) {
            if (!saturationConvergedVerificationStage) {
                if (config.collectControlNodeStatistics()) {
                    stats.getSumOfReceivedAxiomsEqualsSumOfSentAxiomsEvent().getAndIncrement();
                }
                log.info("Sum of received axioms equals sum of sent axioms. Entering verification stage: requesting all axiom message counts.");
                saturationConvergedVerificationStage = true;
                communicationChannel.requestAxiomCountsFromAllWorkers();
            } else if (convergedWorkers.get() == numberOfWorkers) {
                // all workers converged
                onSaturationConverged();
            }
        } else if (saturationConvergedVerificationStage && sumOfAllReceivedAxioms.get() != sumOfAllSentAxioms.get()) {
            log.info("Sum of received axioms does not equal sum of sent axioms anymore.");
            log.info("Received: " + sumOfAllReceivedAxioms.get());
            log.info("Sent: " + sumOfAllSentAxioms.get());
            saturationConvergedVerificationStage = false;
            convergedWorkers.set(0);
        }

    }

    private void onSaturationConverged() {
        if (config.collectControlNodeStatistics()) {
            stats.stopStopwatch(StatisticsComponent.CONTROL_NODE_SATURATION_TIME);
            stats.startStopwatch(StatisticsComponent.CONTROL_NODE_WAITING_FOR_CLOSURE_RESULTS);
        }

        log.info("All workers converged.");
        saturationControlNode.switchState(new CNSWaitingForClosureResults<>(saturationControlNode));
        communicationChannel.broadcast(SaturationStatusMessage.CONTROL_NODE_REQUEST_SEND_CLOSURE_RESULT,
                new Runnable() {
                    @Override
                    public void run() {
                        communicationChannel.getReceivedClosureResultsCounter().getAndIncrement();
                        log.info("(" + communicationChannel.getReceivedClosureResultsCounter()
                                .get() + "/" + numberOfWorkers + ")" +
                                " workers have sent their closure results.");
                    }
                });
    }

}
