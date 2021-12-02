package reasoning.saturation.distributed;

import data.Closure;
import data.DefaultClosure;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
import reasoning.saturation.distributed.states.controlnode.CNSFinished;
import reasoning.saturation.distributed.states.controlnode.CNSInitializing;
import reasoning.saturation.distributed.states.controlnode.ControlNodeState;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class SaturationControlNode {

    private final ControlNodeCommunicationChannel communicationChannel;
    private final Closure closureResult = new DefaultClosure();
    private final List<DistributedWorkerModel> workers;
    private ControlNodeState state;

    protected SaturationControlNode(List<DistributedWorkerModel> workers, WorkloadDistributor workloadDistributor, List<? extends Serializable> initialAxioms) {
        this.communicationChannel = new ControlNodeCommunicationChannel(workers, workloadDistributor, initialAxioms);
        this.workers = workers;
        init();
    }

    private void init() {
        this.state = new CNSInitializing(this);
    }

    public Closure saturate() {
        try {
            while (!(state instanceof CNSFinished)) {
                state.mainControlNodeLoop();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return closureResult;
    }

    public Collection<DistributedWorkerModel> getWorkers() {
        return workers;
    }

    public void switchState(ControlNodeState state) {
        this.state = state;
    }

    public ControlNodeCommunicationChannel getCommunicationChannel() {
        return communicationChannel;
    }

    public void addAxiomsToClosureResult(Collection<? extends Serializable> axioms) {
        this.closureResult.addAll(axioms);
    }
}
