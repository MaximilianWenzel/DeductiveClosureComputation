package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import exceptions.MessageProtocolViolationException;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.messages.*;
import reasoning.reasoner.DefaultIncrementalReasoner;
import reasoning.saturation.distributed.SaturationWorker;
import reasoning.saturation.distributed.communication.WorkerCommunicationChannel;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.distributed.states.AxiomVisitor;
import util.ConsoleUtils;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * This class is a representation of the current state of a given worker in the distributed saturation procedure. Each distinct state
 * provides a state-dependent processing of the received messages.
 *
 * @param <C> Type of the resulting deductive closure.
 * @param <A> Type of the axioms in the deductive closure.
 */
public abstract class WorkerState<C extends Closure<A>, A extends Serializable> implements MessageModelVisitor<C, A>,
        AxiomVisitor<A> {

    protected final Logger log = ConsoleUtils.getLogger();

    protected WorkerStatistics stats;
    protected SaturationConfiguration config;

    protected ExecutorService threadPool;

    protected SaturationWorker<C, A> worker;
    protected WorkerCommunicationChannel<C, A> communicationChannel;
    protected AcknowledgementEventManager acknowledgementEventManager;

    protected DefaultIncrementalReasoner<C, A> incrementalReasoner;

    public WorkerState(SaturationWorker<C, A> worker) {
        this.worker = worker;
        this.communicationChannel = worker.getCommunicationChannel();
        this.incrementalReasoner = worker.getIncrementalReasoner();
        this.acknowledgementEventManager = communicationChannel.getAcknowledgementEventManager();
        this.config = worker.getConfig();
        this.stats = worker.getStats();
        this.threadPool = worker.getThreadPool();
    }

    public void mainWorkerLoop(Object msg) {
        if (msg instanceof MessageModel) {
            ((MessageModel<C, A>) msg).accept(this);
        } else {
            this.visit((A) msg);
        }
    }

    public void onToDoIsEmpty() {
        // do nothing
    }

    @Override
    public void visit(DebugMessage message) {
        log.info(message.getMessage());
    }

    @Override
    public void visit(InitializeWorkerMessage<C, A> message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StateInfoMessage message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(RequestAxiomMessageCount message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(AxiomCount message) {
        throw new MessageProtocolViolationException();
    }

    @Override
    public void visit(StatisticsMessage message) {
        throw new MessageProtocolViolationException();
    }

    protected void messageProtocolViolation(StateInfoMessage message) {
        log.warning("State: " + this.getClass() + ", message type: " + message.getStatusMessage());
        throw new MessageProtocolViolationException();
    }
}
