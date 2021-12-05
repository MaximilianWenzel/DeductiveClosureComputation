package networking.messages;

import data.Closure;
import reasoning.rules.Rule;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class InitializeWorkerMessage<C extends Closure<A>, A extends Serializable> extends MessageModel<C, A> {

    private long workerID;
    private List<DistributedWorkerModel<C, A>> workers;
    private WorkloadDistributor workloadDistributor;
    private Collection<? extends Rule<C, A>> rules;
    private Collection<A> initialAxioms;

    public InitializeWorkerMessage(long senderID,
                                   long workerID,
                                   List<DistributedWorkerModel<C, A>> workers,
                                   WorkloadDistributor workloadDistributor,
                                   Collection<? extends Rule<C, A>> rules,
                                   Collection<A> initialAxioms) {
        super(senderID);
        this.workerID = workerID;
        this.workers = workers;
        this.workloadDistributor = workloadDistributor;
        this.rules = rules;
        this.initialAxioms = initialAxioms;
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

    public WorkloadDistributor getWorkloadDistributor() {
        return workloadDistributor;
    }

    public Collection<A> getInitialAxioms() {
        return initialAxioms;
    }

    public Collection<? extends Rule<C, A>> getRules() {
        return rules;
    }
}
