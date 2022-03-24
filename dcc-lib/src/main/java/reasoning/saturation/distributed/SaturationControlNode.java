package reasoning.saturation.distributed;

import data.Closure;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
import reasoning.saturation.distributed.communication.NIO2NetworkingPipeline;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.DistributedSaturationConfiguration;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This class is the representation of the control node in the distributed saturation which distributes the initial axioms, determines
 * whether all workers have converged, and finally collects the closure results of each worker.
 *
 * @param <C> Type of the resulting deductive closure.
 * @param <A> Type of the axioms in the deductive closure.
 */
public class SaturationControlNode<C extends Closure<A>, A extends Serializable> {

    private final List<DistributedWorkerModel<C, A>> workers;
    private final C resultingClosure;
    private final int numberOfThreads;
    private final WorkloadDistributor<C, A> workloadDistributor;
    private final Iterator<? extends A> initialAxioms;
    private final DistributedSaturationConfiguration config;
    private final ControlNodeStatistics stats = new ControlNodeStatistics();
    private final List<WorkerStatistics> workerStatistics = new ArrayList<>();
    private ControlNodeCommunicationChannel<C, A> communicationChannel;
    private ControlNodeState<C, A> state;
    private ExecutorService threadPool;
    private NIO2NetworkingPipeline networkingLoop;


    protected SaturationControlNode(List<DistributedWorkerModel<C, A>> workers,
                                    WorkloadDistributor<C, A> workloadDistributor,
                                    Iterator<? extends A> initialAxioms,
                                    C resultingClosure,
                                    DistributedSaturationConfiguration config,
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
        this.networkingLoop = new ControlNodeNetworkingPipeline(threadPool);
        this.communicationChannel = new ControlNodeCommunicationChannel<>(
                workers, workloadDistributor, initialAxioms,
                config, networkingLoop);
        CNSInitializing<C, A> initState = new CNSInitializing<>(this);
        this.state = initState;
        initState.start();
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

    public Collection<DistributedWorkerModel<C, A>> getWorkers() {
        return workers;
    }

    public void switchState(ControlNodeState<C, A> state) {
        this.state = state;
    }

    public ControlNodeCommunicationChannel<C, A> getCommunicationChannel() {
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


    private class ControlNodeNetworkingPipeline extends NIO2NetworkingPipeline {

        public ControlNodeNetworkingPipeline(ExecutorService threadPool) {
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
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
