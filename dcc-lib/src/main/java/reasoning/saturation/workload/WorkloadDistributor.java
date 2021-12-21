package reasoning.saturation.workload;

import data.Closure;
import reasoning.saturation.models.WorkerModel;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public abstract class WorkloadDistributor<C extends Closure<A>, A extends Serializable, T extends Serializable> implements Serializable {

    protected List<? extends WorkerModel<C, A, T>> workerModels;

    protected WorkloadDistributor() {
    }

    public WorkloadDistributor(List<? extends WorkerModel<C, A, T>> workerModels) {
        this.workerModels = workerModels;
    }

    public abstract List<Long> getRelevantWorkerIDsForAxiom(A axiom);
}
