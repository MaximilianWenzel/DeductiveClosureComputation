package reasoning.saturation.distributed;

import data.Closure;
import reasoning.saturation.Saturation;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.List;

public class DistributedSaturation<C extends Closure<A>, A extends Serializable, T extends Serializable>
        implements Saturation<C, A> {

    protected WorkloadDistributor<C, A, T> workloadDistributor;
    protected List<DistributedWorkerModel<C, A, T>> workers;
    protected SaturationControlNode<C, A, T> controlNode;
    protected SaturationConfiguration config;
    protected int numberOfThreadsForControlNode;

    public DistributedSaturation(List<DistributedWorkerModel<C, A, T>> workers,
                                 WorkloadDistributor<C, A, T> workloadDistributor,
                                 List<? extends A> initialAxioms,
                                 C resultingClosure,
                                 int numberOfThreadsForControlNode) {
        this.workloadDistributor = workloadDistributor;
        this.workers = workers;
        this.config = new SaturationConfiguration();
        this.controlNode = new SaturationControlNode<>(workers, workloadDistributor, initialAxioms, resultingClosure,
                config, numberOfThreadsForControlNode);
    }

    public DistributedSaturation(List<DistributedWorkerModel<C, A, T>> workers,
                                 WorkloadDistributor<C, A, T> workloadDistributor,
                                 List<? extends A> initialAxioms,
                                 C resultingClosure,
                                 SaturationConfiguration config,
                                 int numberOfThreadsForControlNode) {
        this.workloadDistributor = workloadDistributor;
        this.workers = workers;
        this.config = config;
        this.controlNode = new SaturationControlNode<>(workers, workloadDistributor, initialAxioms, resultingClosure,
                config, numberOfThreadsForControlNode);
    }


    @Override
    public C saturate() {
        return this.controlNode.saturate();
    }

    public ControlNodeStatistics getControlNodeStatistics() {
        return controlNode.getControlNodeStatistics();
    }

    public List<WorkerStatistics> getWorkerStatistics() {
        return controlNode.getWorkerStatistics();
    }

}
