package reasoning.saturation.workload;

import data.Closure;
import reasoning.saturation.models.WorkerModel;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;

public abstract class WorkloadDistributor<C extends Closure<A>, A extends Serializable> implements Serializable {



    public WorkloadDistributor() {
    }

    public abstract Stream<Long> getRelevantWorkerIDsForAxiom(A axiom);
}
