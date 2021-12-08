package reasoning.saturation;

import data.Closure;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public abstract class SaturationInitializationFactory<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    public abstract List<WorkerModel<C, A, T>> getWorkerModels();
    public abstract List<? extends A> getInitialAxioms();
    public abstract C getNewClosure();
    public abstract WorkloadDistributor<C, A, T> getWorkloadDistributor();
}
