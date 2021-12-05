package reasoning.saturation;

import data.Closure;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public abstract class SaturationInitializationFactory<C extends Closure<A>, A extends Serializable> {

    public abstract Collection<WorkerModel<C, A>> getWorkerModels();
    public abstract List<A> getInitialAxioms();
    public abstract C getNewClosure();
    public abstract WorkloadDistributor getWorkloadDistributor();
}
