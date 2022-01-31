package reasoning.saturation.distributed.states.workernode;

import data.Closure;
import exceptions.MessageProtocolViolationException;
import networking.acknowledgement.AcknowledgementEventManager;
import networking.messages.*;
import reasoning.reasoner.IncrementalStreamReasoner;
import reasoning.saturation.distributed.SaturationWorker;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.distributed.states.AxiomVisitor;
import util.ConsoleUtils;

import java.io.Serializable;
import java.util.logging.Logger;
import java.util.stream.Stream;

public abstract class WorkerState<C extends Closure<A>, A extends Serializable, T extends Serializable>
        implements MessageModelVisitor<C, A, T>,
        AxiomVisitor<A> {

    protected final Logger log = ConsoleUtils.getLogger();

    protected WorkerStatistics stats;
    protected SaturationConfiguration config;

    protected IncrementalStreamReasoner<C, A> incrementalReasoner;

    protected SaturationWorker<C, A, T> worker;
    protected AcknowledgementEventManager acknowledgementEventManager;

    public WorkerState(SaturationWorker<C, A, T> worker) {
        this.worker = worker;
        this.acknowledgementEventManager = worker.getAcknowledgementEventManager();
        this.incrementalReasoner = worker.getIncrementalReasoner();
        this.config = worker.getConfig();
        this.stats = worker.getStats();
    }

    public void processMessage(Object msg) {
        if (msg instanceof MessageModel) {
            ((MessageModel<C, A, T>) msg).accept(this);
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
    public void visit(InitializeWorkerMessage<C, A, T> message) {
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
