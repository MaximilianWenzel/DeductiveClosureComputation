package reasoning.saturation.distributed;

import data.Closure;
import reasoning.saturation.Saturation;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.List;

public class DistributedSaturation<C extends Closure<A>, A extends Serializable> implements Saturation<C, A> {

    protected WorkloadDistributor workloadDistributor;
    protected List<DistributedWorkerModel<C, A>> workers;
    protected SaturationControlNode<C, A> controlNode;

    public DistributedSaturation(List<DistributedWorkerModel<C, A>> workers,
                                 WorkloadDistributor workloadDistributor,
                                 List<A> initialAxioms,
                                 C resultingClosure) {
        this.workloadDistributor = workloadDistributor;
        this.workers = workers;
        this.controlNode = new SaturationControlNode<>(workers, workloadDistributor, initialAxioms, resultingClosure);
    }

    @Override
    public C saturate() {
        return this.controlNode.saturate();
    }

}
