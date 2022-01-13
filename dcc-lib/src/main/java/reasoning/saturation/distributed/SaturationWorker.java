package reasoning.saturation.distributed;

import data.Closure;
import exceptions.NotImplementedException;
import networking.ServerData;
import networking.messages.InitializeWorkerMessage;
import reasoning.reasoner.IncrementalStreamReasoner;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.communication.WorkerNodeCommunicationChannel;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.distributed.states.workernode.WorkerState;
import reasoning.saturation.distributed.states.workernode.WorkerStateFinished;
import reasoning.saturation.distributed.states.workernode.WorkerStateInitializing;
import util.ConsoleUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class SaturationWorker<C extends Closure<A>, A extends Serializable, T extends Serializable>
        implements Runnable {

    private static final Logger log = ConsoleUtils.getLogger();
    private final IncrementalReasonerType incrementalReasonerType;
    private ServerData serverData;
    private C closure;
    private Collection<? extends Rule<C, A>> rules;

    private WorkerNodeCommunicationChannel<C, A, T> communicationChannel;
    private int numberOfThreads;
    private ExecutorService threadPool = null;

    private WorkerState<C, A, T> state;
    private IncrementalStreamReasoner<C, A> incrementalReasoner;
    private SaturationConfiguration config;
    private WorkerStatistics stats = new WorkerStatistics();
    private boolean terminateAfterSaturation = false;

    public SaturationWorker(ServerData serverData,
                            IncrementalReasonerType incrementalReasonerType,
                            int numberOfThreads) {
        this.serverData = serverData;
        this.numberOfThreads = numberOfThreads;
        this.incrementalReasonerType = incrementalReasonerType;
        init();
    }

    public SaturationWorker(ServerData serverData,
                            IncrementalReasonerType incrementalReasonerType,
                            int numberOfThreads,
                            boolean terminateAfterSaturation) {
        this(serverData, incrementalReasonerType, numberOfThreads);
        this.terminateAfterSaturation = terminateAfterSaturation;
        init();
    }

    public static void main(String[] args) {
        // args: <HOSTNAME> <PORT-NUMBER> <NUMBER-OF-NETWORKING-THREADS>

        log.info("Generating worker...");

        if (args.length != 3) {
            throw new IllegalArgumentException("arguments: <HOSTNAME> <PORT-NUMBER> <NUMBER-OF-NETWORKING-THREADS>");
        }

        String hostname = args[0];
        int portNumber = Integer.parseInt(args[1]);
        int numberOfNetworkingThreads = Integer.parseInt(args[2]);

        ServerData serverData = new ServerData(hostname, portNumber);

        SaturationWorker<?, ?, ?> saturationWorker = new SaturationWorker<>(
                serverData,
                IncrementalReasonerType.SINGLE_THREADED,
                numberOfNetworkingThreads,
                false
        );
        saturationWorker.run();
    }

    private void init() {
        if (threadPool == null) {
            threadPool = Executors.newFixedThreadPool(numberOfThreads);
        }
        this.communicationChannel = new WorkerNodeCommunicationChannel<>(serverData, threadPool);
        this.state = new WorkerStateInitializing<>(this);

    }

    @Override
    public void run() {
        try {
            do {
                while (!(state instanceof WorkerStateFinished)) {
                    state.mainWorkerLoop();
                }
                log.info("Saturation finished.");
                clearWorkerForNewSaturation();
            } while (!terminateAfterSaturation);

        } catch (InterruptedException e) {
            log.info("Worker has been interrupted.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            log.info("Terminating worker...");
            terminate();
            log.info("Worker terminated.");
        }
    }

    private void clearWorkerForNewSaturation() {
        log.info("Restarting worker...");
        communicationChannel.terminateNow();
        init();
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

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public void terminate() {
        communicationChannel.terminateNow();
    }

    public SaturationConfiguration getConfig() {
        return config;
    }

    public WorkerStatistics getStats() {
        return stats;
    }


    public enum IncrementalReasonerType {
        SINGLE_THREADED,
        PARALLEL;
    }
}
