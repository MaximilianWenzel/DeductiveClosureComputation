package eldlreasoning;

import eldlsyntax.ELConceptExistentialRestriction;
import eldlsyntax.ELConceptInclusion;
import reasoning.saturation.models.PartitionModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OWLELWorkloadDistributor extends WorkloadDistributor {

    public OWLELWorkloadDistributor(List<PartitionModel> partitionModels) {
        super(partitionModels);
    }

    @Override
    public List<Long> getRelevantPartitionIDsForAxiom(Object axiom) {
        List<Long> partitionIDs = new ArrayList<>();
        ELConceptInclusion conceptInclusion = (ELConceptInclusion) axiom;
        for (PartitionModel partition : partitionModels) {
            if (isRelevantAxiomToPartition(partition, conceptInclusion)) {
                partitionIDs.add(partition.getID());
            }
        }
        return partitionIDs;
    }

    @Override
    public boolean isRelevantAxiomToPartition(PartitionModel partition, ELConceptInclusion axiom) {
        Set<?> partitionTerms = partition.getPartitionTerms();
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
