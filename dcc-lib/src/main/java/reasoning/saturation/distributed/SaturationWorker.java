package reasoning.saturation.distributed;

import data.Closure;
import exceptions.NotImplementedException;
import networking.ServerData;
import networking.messages.InitializeWorkerMessage;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reasoning.reasoner.IncrementalStreamReasoner;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.communication.WorkerNodeCommunicationChannel;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.distributed.states.workernode.WorkerState;
import reasoning.saturation.distributed.states.workernode.WorkerStateInitializing;
import util.ConsoleUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class SaturationWorker<C extends Closure<A>, A extends Serializable, T extends Serializable>
        implements Subscriber<Object> {

    private static final Logger log = ConsoleUtils.getLogger();
    private final IncrementalReasonerType incrementalReasonerType;
    private ServerData serverData;
    private C closure;
    private Collection<? extends Rule<C, A>> rules;

    private WorkerNodeCommunicationChannel<C, A, T> communicationChannel;

    private WorkerState<C, A, T> state;
    private IncrementalStreamReasoner<C, A> incrementalReasoner;
    private SaturationConfiguration config;
    private WorkerStatistics stats = new WorkerStatistics();
    private AtomicBoolean workerIsStarted = new AtomicBoolean(false);
    private boolean terminateAfterSaturation = false;

    private Subscription receivedMessagesSubscription;

    public SaturationWorker(ServerData serverData,
                            IncrementalReasonerType incrementalReasonerType) {
        this.serverData = serverData;
        this.incrementalReasonerType = incrementalReasonerType;
        init();
    }

    public SaturationWorker(ServerData serverData,
                            IncrementalReasonerType incrementalReasonerType,
                            boolean terminateAfterSaturation) {
        this(serverData, incrementalReasonerType);
        this.terminateAfterSaturation = terminateAfterSaturation;
    }

    public static void main(String[] args) {
        // args: <HOSTNAME> <PORT-NUMBER>
        log.info("Generating worker...");

        if (args.length != 3) {
            throw new IllegalArgumentException("arguments: <HOSTNAME> <PORT-NUMBER>");
        }

        String hostname = args[0];
        int portNumber = Integer.parseInt(args[1]);

        ServerData serverData = new ServerData(hostname, portNumber);

        SaturationWorker<?, ?, ?> saturationWorker = new SaturationWorker<>(
                serverData,
                IncrementalReasonerType.SINGLE_THREADED,
                true
        );
        saturationWorker.start();
    }

    private void init() {
        this.communicationChannel = new WorkerNodeCommunicationChannel<>(serverData, this);
        this.state = new WorkerStateInitializing<>(this);
    }

    private void clearWorkerForNewSaturation() {
        log.info("Restarting worker...");
        communicationChannel.reset();
        this.state = new WorkerStateInitializing<>(this);
        this.rules = null;
        this.config = null;
        this.stats = new WorkerStatistics();
        log.info("Worker successfully restarted.");
    }

    public C getClosure() {
        return closure;
    }

    public void switchState(WorkerState<C, A, T> newState) {
        this.state = newState;
    }

    public WorkerNodeCommunicationChannel<C, A, T> getCommunicationChannel() {
        return communicationChannel;
    }

    public IncrementalStreamReasoner<C, A> getIncrementalReasoner() {
        return incrementalReasoner;
    }

    public void initializeWorker(InitializeWorkerMessage<C, A, T> message) {
        this.communicationChannel.setWorkerID(message.getWorkerID());
        this.communicationChannel.setWorkers(message.getWorkers());
        this.communicationChannel.setWorkloadDistributor(message.getWorkloadDistributor());
        this.communicationChannel.setConfig(message.getConfig());
        this.communicationChannel.setStats(this.stats);
        this.closure = message.getClosure();
        this.config = message.getConfig();

        this.setRules(message.getRules());
    }

    public void setRules(Collection<? extends Rule<C, A>> rules) {
        this.rules = rules;
        initializeRules();
    }

    public void start() {
        if (workerIsStarted.compareAndSet(false, true)) {
            if (receivedMessagesSubscription != null) {
                receivedMessagesSubscription.request(1);
            }
        }
    }

    public void stop() {
        communicationChannel.terminate();
    }


    private void initializeRules() {
        this.rules.forEach(r -> {
            r.setClosure(closure);
        });

        switch (incrementalReasonerType) {
            case SINGLE_THREADED:
                this.incrementalReasoner = new IncrementalStreamReasoner<>(rules, closure, config, stats);
                break;
            default:
                throw new NotImplementedException();
        }
    }

    public void terminate() {
        communicationChannel.terminate();
    }

    public SaturationConfiguration getConfig() {
        return config;
    }

    public WorkerStatistics getStats() {
        return stats;
    }

    public WorkerState<C, A, T> getState() {
        return state;
    }

    public enum IncrementalReasonerType {
        SINGLE_THREADED,
        PARALLEL;
    }


    @Override
    public void onSubscribe(Subscription subscription) {
        this.receivedMessagesSubscription = subscription;
        if (this.workerIsStarted.get()) {
            this.receivedMessagesSubscription.request(1);
        }
    }

    @Override
    public void onNext(Object o) {
        state.processMessage(o); // TODO
        this.receivedMessagesSubscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        log.warning(throwable.getMessage());
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
        log.info("Saturation finished.");
        if (terminateAfterSaturation) {
            this.communicationChannel.terminate();
        } else {
            clearWorkerForNewSaturation();
        }
    }
}
