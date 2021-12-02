package reasoning.saturation.workload;

import eldlsyntax.ELConceptInclusion;
import reasoning.saturation.models.WorkerModel;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public abstract class WorkloadDistributor implements Serializable {

    protected Collection<? extends WorkerModel> workerModels;

    public WorkloadDistributor(Collection<? extends WorkerModel> workerModels) {
        this.workerModels = workerModels;
    }

    public abstract List<Long> getRelevantPartitionIDsForAxiom(Object axiom);

    public abstract boolean isRelevantAxiomToPartition(WorkerModel worker, ELConceptInclusion axiom);
}
