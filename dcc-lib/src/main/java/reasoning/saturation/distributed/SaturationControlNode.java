package reasoning.saturation.distributed;

import data.Closure;
import networking.messages.MessageEnvelope;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
import reasoning.saturation.distributed.communication.NIO2NetworkingLoop;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.distributed.states.controlnode.CNSFinished;
import reasoning.saturation.distributed.states.controlnode.CNSInitializing;
import reasoning.saturation.distributed.states.controlnode.ControlNodeState;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

public class SaturationControlNode<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private final List<DistributedWorkerModel<C, A, T>> workers;
    private C resultingClosure;
    private ControlNodeCommunicationChannel<C, A, T> communicationChannel;
    private BlockingQueue<MessageEnvelope> messagesThatCouldNotBeSent = new LinkedBlockingQueue<>();
    private ControlNodeState<C, A, T> state;

    private int numberOfThreads;
    private WorkloadDistributor<C, A, T> workloadDistributor;
    private Iterator<? extends A> initialAxioms;
    private SaturationConfiguration config;
    private ControlNodeStatistics stats = new ControlNodeStatistics();
    private List<WorkerStatistics> workerStatistics = new ArrayList<>();

    private ExecutorService threadPool;
    private NIO2NetworkingLoop networkingLoop;


    protected SaturationControlNode(List<DistributedWorkerModel<C, A, T>> workers,
                                    WorkloadDistributor<C, A, T> workloadDistributor,
                                    Iterator<? extends A> initialAxioms,
                                    C resultingClosure,
                                    SaturationConfiguration config,
                                    int numberOfThreads) {
        this.workers = workers;
        this.workloadDistributor = workloadDistributor;
        this.initialAxioms = initialAxioms;
        this.resultingClosure = resultingClosure;
        this.config = config;
        this.numberOfThreads = numberOfThreads;
        init();
    }

    private void init() {
        this.threadPool = Executors.newFixedThreadPool(numberOfThreads);
        this.networkingLoop = new ControlNodeNetworkingLoop(threadPool);
        this.communicationChannel = new ControlNodeCommunicationChannel<>(
                workers, workloadDistributor, initialAxioms,
                config, networkingLoop);
        this.state = new CNSInitializing<>(this);
    }

    public C saturate() {
        networkingLoop.start();
        try {
            threadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return resultingClosure;
    }

    public Collection<DistributedWorkerModel<C, A, T>> getWorkers() {
        return workers;
    }

    public void switchState(ControlNodeState<C, A, T> state) {
        this.state = state;
    }

    public ControlNodeCommunicationChannel<C, A, T> getCommunicationChannel() {
        return communicationChannel;
    }

    public void addAxiomToClosureResult(A axiom) {
        this.resultingClosure.add(axiom);
    }

    public SaturationConfiguration getConfig() {
        return config;
    }

    public ControlNodeStatistics getControlNodeStatistics() {
        return stats;
    }

    public List<WorkerStatistics> getWorkerStatistics() {
        return workerStatistics;
    }


    private class ControlNodeNetworkingLoop extends NIO2NetworkingLoop {

        public ControlNodeNetworkingLoop(ExecutorService threadPool) {
            super(threadPool, true);
        }

        @Override
        public void onRestart() {
        }

        @Override
        public void onNoMoreMessages() {
        }

        @Override
        public void onTerminate() {
            if (config.collectControlNodeStatistics()) {
                stats.collectStopwatchTimes();
            }
            communicationChannel.terminate();
            threadPool.shutdownNow();
        }

        @Override
        public boolean runningCondition() {
            return !(state instanceof CNSFinished);
        }

        @Override
        public void processNextMessage(Object nextMessage) {
            state.mainControlNodeLoop(nextMessage);
        }
    }
}
