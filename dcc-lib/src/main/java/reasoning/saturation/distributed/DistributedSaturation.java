package reasoning.saturation.distributed;

import data.Closure;
import enums.MessageDistributionType;
import reasoning.saturation.Saturation;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.DistributedSaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * A class that can be used in order to compute the deductive closure for a given set of axioms and rules in a distributed manner. This
 * class represents the entry point of the control node, i.e., the worker nodes have to be started in a separate thread/process and must be
 * available on the specified network address and port.
 *
 * @param <C> Type of the resulting deductive closure.
 * @param <A> Type of the axioms in the deductive closure.
 */
public class DistributedSaturation<C extends Closure<A>, A extends Serializable>
        implements Saturation<C, A> {

    protected WorkloadDistributor<C, A> workloadDistributor;
    protected List<DistributedWorkerModel<C, A>> workers;
    protected SaturationControlNode<C, A> controlNode;
    protected DistributedSaturationConfiguration config;

    public DistributedSaturation(List<DistributedWorkerModel<C, A>> workers,
                                 WorkloadDistributor<C, A> workloadDistributor,
                                 Iterator<? extends A> initialAxioms,
                                 C resultingClosure,
                                 int numberOfThreadsForControlNode) {
        this.workloadDistributor = workloadDistributor;
        this.workers = workers;
        this.config = new DistributedSaturationConfiguration(true, false, MessageDistributionType.ADD_OWN_MESSAGES_DIRECTLY_TO_TODO);
        this.controlNode = new SaturationControlNode<>(workers, workloadDistributor, initialAxioms, resultingClosure,
                config, numberOfThreadsForControlNode);
    }

    public DistributedSaturation(List<DistributedWorkerModel<C, A>> workers,
                                 WorkloadDistributor<C, A> workloadDistributor,
                                 Iterator<? extends A> initialAxioms,
                                 C resultingClosure,
                                 DistributedSaturationConfiguration config,
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
