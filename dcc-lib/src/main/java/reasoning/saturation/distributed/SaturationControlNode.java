package reasoning.saturation.distributed;

import data.Closure;
import reasoning.saturation.distributed.communication.BenchmarkConfiguration;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
import reasoning.saturation.distributed.states.controlnode.CNSFinished;
import reasoning.saturation.distributed.states.controlnode.CNSInitializing;
import reasoning.saturation.distributed.states.controlnode.ControlNodeState;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class SaturationControlNode<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private final ControlNodeCommunicationChannel<C, A, T> communicationChannel;
    private final List<DistributedWorkerModel<C, A, T>> workers;
    private C resultingClosure;
    private ControlNodeState<C, A, T> state;

    protected SaturationControlNode(List<DistributedWorkerModel<C, A, T>> workers,
                                    WorkloadDistributor<C, A, T> workloadDistributor,
                                    List<? extends A> initialAxioms,
                                    C resultingClosure) {
        this.communicationChannel = new ControlNodeCommunicationChannel<>(workers, workloadDistributor, initialAxioms);
        this.workers = workers;
        this.resultingClosure = resultingClosure;
        init();
    }

    protected SaturationControlNode(BenchmarkConfiguration benchmarkConfiguration,
                                    List<DistributedWorkerModel<C, A, T>> workers,
                                    WorkloadDistributor<C, A, T> workloadDistributor,
                                    List<? extends A> initialAxioms,
                                    C resultingClosure) {
        this.communicationChannel = new ControlNodeCommunicationChannel<>(benchmarkConfiguration, workers, workloadDistributor, initialAxioms);
        this.workers = workers;
        this.resultingClosure = resultingClosure;
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
            communicationChannel.terminate();
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

    public void addAxiomsToClosureResult(Collection<A> axioms) {
        for (A axiom : axioms) {
            this.resultingClosure.add(axiom);
        }
    }
}
