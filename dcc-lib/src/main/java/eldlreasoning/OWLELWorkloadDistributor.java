package eldlreasoning;

import eldlsyntax.ELConceptExistentialRestriction;
import eldlsyntax.ELConceptInclusion;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class OWLELWorkloadDistributor extends WorkloadDistributor {

    public OWLELWorkloadDistributor(Collection<? extends WorkerModel> partitionModels) {
        super(partitionModels);
    }

    @Override
    public List<Long> getRelevantPartitionIDsForAxiom(Object axiom) {
        List<Long> partitionIDs = new ArrayList<>();
        ELConceptInclusion conceptInclusion = (ELConceptInclusion) axiom;
        for (WorkerModel partition : workerModels) {
            if (isRelevantAxiomToPartition(partition, conceptInclusion)) {
                partitionIDs.add(partition.getID());
            }
        }
        return partitionIDs;
    }

    @Override
    public boolean isRelevantAxiomToPartition(WorkerModel worker, ELConceptInclusion axiom) {
        Set<?> partitionTerms = worker.getWorkerTerms();
        if (partitionTerms.contains(axiom.getSubConcept())
                || partitionTerms.contains(axiom.getSuperConcept())) {
            return true;
        } else if (axiom.getSuperConcept() instanceof ELConceptExistentialRestriction) {
            ELConceptExistentialRestriction exist = (ELConceptExistentialRestriction) axiom.getSuperConcept();
            if (partitionTerms.contains(exist.getFiller())) {
                return true;
            }
        }
        return false;
    }

}
