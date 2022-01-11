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

public class SaturationControlNode<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private final ControlNodeCommunicationChannel<C, A, T> communicationChannel;
    private final List<DistributedWorkerModel<C, A, T>> workers;
    private C resultingClosure;
    private ControlNodeState<C, A, T> state;
    private SaturationConfiguration config;
    private ControlNodeStatistics stats = new ControlNodeStatistics();
    private List<WorkerStatistics> workerStatistics = new ArrayList<>();

    protected SaturationControlNode(List<DistributedWorkerModel<C, A, T>> workers,
                                    WorkloadDistributor<C, A, T> workloadDistributor,
                                    List<? extends A> initialAxioms,
                                    C resultingClosure,
                                    SaturationConfiguration config) {
        this.communicationChannel = new ControlNodeCommunicationChannel<>(workers, workloadDistributor, initialAxioms, config);
        this.workers = workers;
        this.resultingClosure = resultingClosure;
        this.config = config;
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
