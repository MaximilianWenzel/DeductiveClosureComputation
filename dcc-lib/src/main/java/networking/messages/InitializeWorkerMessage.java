package networking.messages;

import data.Closure;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.metadata.DistributedSaturationConfiguration;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * A initialization message which is sent from the control node to a worker node. It determines which rules are assigned to the worker and
 * how newly deduced conclusions are distributed to the workers.
 *
 * @param <C> Type of the resulting deductive closure.
 * @param <A> Type of the axioms in the deductive closure.
 */
public class InitializeWorkerMessage<C extends Closure<A>, A extends Serializable>
        extends MessageModel<C, A> {

    private long workerID;
    private List<DistributedWorkerModel<C, A>> workers;
    private WorkloadDistributor<C, A> workloadDistributor;
    private List<? extends Rule<C, A>> rules;
    private DistributedSaturationConfiguration config;
    private C closure;

    protected InitializeWorkerMessage() {
    }

    public InitializeWorkerMessage(long senderID, long workerID,
                                   List<DistributedWorkerModel<C, A>> workers,
                                   WorkloadDistributor<C, A> workloadDistributor,
                                   C closure, List<? extends Rule<C, A>> rules,
                                   DistributedSaturationConfiguration config) {
        super(senderID);
        this.workerID = workerID;
        this.workers = workers;
        this.workloadDistributor = workloadDistributor;
        this.closure = closure;
        this.rules = rules;
        this.config = config;
    }

    @Override
    public void accept(MessageModelVisitor<C, A> visitor) {
        visitor.visit(this);
    }

    public long getWorkerID() {
        return workerID;
    }

    public List<DistributedWorkerModel<C, A>> getWorkers() {
        return workers;
    }

    public WorkloadDistributor<C, A> getWorkloadDistributor() {
        return workloadDistributor;
    }

    public Collection<? extends Rule<C, A>> getRules() {
        return rules;
    }

    public DistributedSaturationConfiguration getConfig() {
        return config;
    }

    public C getClosure() {
        return closure;
    }
}
