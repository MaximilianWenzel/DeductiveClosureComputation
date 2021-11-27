package reasoning.saturation.workload;

import eldlsyntax.ELConceptInclusion;
import reasoning.saturation.models.PartitionModel;

import java.util.List;

public abstract class WorkloadDistributor {

    protected List<? extends PartitionModel> partitionModels;

    public WorkloadDistributor(List<? extends PartitionModel> partitionModels) {
        this.partitionModels = partitionModels;
    }

    public abstract List<Long> getRelevantPartitionIDsForAxiom(Object axiom);

    public abstract boolean isRelevantAxiomToPartition(PartitionModel partition, ELConceptInclusion axiom);
}
