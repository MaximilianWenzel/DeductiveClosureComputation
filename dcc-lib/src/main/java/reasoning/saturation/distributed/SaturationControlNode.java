package reasoning.saturation.distributed;

import data.Closure;
import data.DefaultClosure;
import reasoning.saturation.distributed.communication.ControlNodeCommunicationChannel;
import reasoning.saturation.distributed.states.controlnode.CNSFinished;
import reasoning.saturation.distributed.states.controlnode.CNSInitializing;
import reasoning.saturation.distributed.states.controlnode.ControlNodeState;
import reasoning.saturation.models.DistributedPartitionModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.Collection;
import java.util.List;

public class SaturationControlNode {

    private final ControlNodeCommunicationChannel communicationChannel;
    private final Closure closureResult = new DefaultClosure();
    private final List<DistributedPartitionModel> partitions;
    private ControlNodeState state;

    protected SaturationControlNode(List<DistributedPartitionModel> partitions, WorkloadDistributor workloadDistributor, List<Object> initialAxioms) {
        this.communicationChannel = new ControlNodeCommunicationChannel(partitions, workloadDistributor, initialAxioms);
        this.partitions = partitions;
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

    public List<DistributedPartitionModel> getPartitions() {
        return partitions;
    }

    public void switchState(ControlNodeState state) {
        this.state = state;
    }

    public ControlNodeCommunicationChannel getCommunicationChannel() {
        return communicationChannel;
    }

    public void addAxiomsToClosureResult(Collection<Object> axioms) {
        this.closureResult.add(axioms);
    }
}
