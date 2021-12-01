package reasoning.saturation.workload;

import eldlsyntax.ELConceptInclusion;
import reasoning.saturation.models.PartitionModel;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public abstract class WorkloadDistributor implements Serializable {

    protected Collection<? extends PartitionModel> partitionModels;

    public WorkloadDistributor(Collection<? extends PartitionModel> partitionModels) {
        this.partitionModels = partitionModels;
    }

    public abstract List<Long> getRelevantPartitionIDsForAxiom(Object axiom);

    public abstract boolean isRelevantAxiomToPartition(PartitionModel partition, ELConceptInclusion axiom);
}
