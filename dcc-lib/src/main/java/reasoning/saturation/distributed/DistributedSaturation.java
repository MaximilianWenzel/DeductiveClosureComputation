package reasoning.saturation.distributed;

import data.Closure;
import reasoning.saturation.Saturation;
import reasoning.saturation.distributed.communication.BenchmarkConfiguration;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.List;

public class DistributedSaturation<C extends Closure<A>, A extends Serializable, T extends Serializable> implements Saturation<C, A> {

    protected WorkloadDistributor<C, A, T> workloadDistributor;
    protected List<DistributedWorkerModel<C, A, T>> workers;
    protected SaturationControlNode<C, A, T> controlNode;

    public DistributedSaturation(List<DistributedWorkerModel<C, A, T>> workers,
                                 WorkloadDistributor<C, A, T> workloadDistributor,
                                 List<? extends A> initialAxioms,
                                 C resultingClosure) {
        this.workloadDistributor = workloadDistributor;
        this.workers = workers;
        this.controlNode = new SaturationControlNode<>(workers, workloadDistributor, initialAxioms, resultingClosure);
    }

    public DistributedSaturation(BenchmarkConfiguration benchmarkConfiguration,
                                 List<DistributedWorkerModel<C, A, T>> workers,
                                 WorkloadDistributor<C, A, T> workloadDistributor,
                                 List<? extends A> initialAxioms,
                                 C resultingClosure) {
        this.workloadDistributor = workloadDistributor;
        this.workers = workers;
        this.controlNode = new SaturationControlNode<>(benchmarkConfiguration, workers, workloadDistributor, initialAxioms, resultingClosure);
    }

    @Override
    public C saturate() {
        return this.controlNode.saturate();
    }

}
