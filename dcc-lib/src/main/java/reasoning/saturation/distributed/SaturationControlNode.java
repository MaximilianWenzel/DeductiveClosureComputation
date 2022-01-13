package reasoning.saturation.distributed;

import data.Closure;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SaturationControlNode<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private C resultingClosure;

    private final List<DistributedWorkerModel<C, A, T>> workers;

    private ControlNodeCommunicationChannel<C, A, T> communicationChannel;
    private ControlNodeState<C, A, T> state;

    private SaturationConfiguration config;
    private ControlNodeStatistics stats = new ControlNodeStatistics();
    private List<WorkerStatistics> workerStatistics = new ArrayList<>();

    private ExecutorService threadPool;
    private int numberOfThreads;

    protected SaturationControlNode(List<DistributedWorkerModel<C, A, T>> workers,
                                    WorkloadDistributor<C, A, T> workloadDistributor,
                                    List<? extends A> initialAxioms,
                                    C resultingClosure,
                                    SaturationConfiguration config,
                                    int numberOfThreads) {
        this.workers = workers;
        this.resultingClosure = resultingClosure;
        this.config = config;
        this.numberOfThreads = numberOfThreads;
        this.threadPool = Executors.newFixedThreadPool(numberOfThreads);
        this.communicationChannel = new ControlNodeCommunicationChannel<>(workers, workloadDistributor, initialAxioms, config, threadPool);
        init();
    }

    private void init() {
        this.state = new CNSInitializing<>(this);
    }

    public C saturate() {
        try {
            while (!(state instanceof CNSFinished)) {
                state.mainControlNodeLoop();
            }
            if (config.collectControlNodeStatistics()) {
                stats.collectStopwatchTimes();
            }
            communicationChannel.terminateAfterAllMessagesHaveBeenSent();
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
}
