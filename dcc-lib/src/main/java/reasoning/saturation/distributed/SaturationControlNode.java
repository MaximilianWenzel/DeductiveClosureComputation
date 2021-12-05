package reasoning.saturation.distributed;

import data.Closure;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
import reasoning.saturation.distributed.states.controlnode.CNSFinished;
import reasoning.saturation.distributed.states.controlnode.CNSInitializing;
import reasoning.saturation.distributed.states.controlnode.ControlNodeState;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class SaturationControlNode<C extends Closure<A>, A extends Serializable> {

    private final ControlNodeCommunicationChannel<C, A> communicationChannel;
    private final List<DistributedWorkerModel<C, A>> workers;
    private C resultingClosure;
    private ControlNodeState<C, A> state;

    protected SaturationControlNode(List<DistributedWorkerModel<C, A>> workers,
                                    WorkloadDistributor workloadDistributor,
                                    List<A> initialAxioms,
                                    C resultingClosure) {
        this.communicationChannel = new ControlNodeCommunicationChannel<>(workers, workloadDistributor, initialAxioms);
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

    public void addAxiomsToClosureResult(Collection<A> axioms) {
        for (A axiom : axioms) {
            this.resultingClosure.add(axiom);
        }
    }
}
