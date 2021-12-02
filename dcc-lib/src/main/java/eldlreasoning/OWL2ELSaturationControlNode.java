package eldlreasoning;

import reasoning.saturation.distributed.SaturationControlNode;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.*;

public class OWL2ELSaturationControlNode extends SaturationControlNode {
    protected OWL2ELSaturationControlNode(List<DistributedWorkerModel> partitions, WorkloadDistributor workloadDistributor, List<? extends Serializable> initialAxioms) {
        super(partitions, workloadDistributor, initialAxioms);
    }

    /*
    private int numberOfPartitions;
    private IndexedELOntology elOntology;

    public OWL2ELSaturationControlNode(IndexedELOntology elOntology, int numberOfPartitions) {
        super(elOntology);
        this.elOntology = elOntology;
        this.numberOfPartitions = numberOfPartitions;
    }

    protected Dataset getDatasetFragmentForPartition(Set<ELConcept> termPartition) {
        IndexedELOntology ontologyFragment = elOntology.getOntologyWithIndexedNegativeConcepts();
        for (ELConceptInclusion axiom : elOntology.getOntologyAxioms()) {
            if (this.isRelevantAxiomToPartition(termPartition, axiom)) {
                ontologyFragment.add(axiom);
            }
        }
        return ontologyFragment;
    }

    @Override
    protected List<PartitionModel> initializePartitions() {

        Iterator<ELConcept> occurringConceptsInDataset = this.elOntology.getAllUsedConceptsInOntology().iterator();

        // create concept partitions
        List<Set<ELConcept>> conceptPartitions = new ArrayList<>(numberOfPartitions);
        for (int i = 0; i < numberOfPartitions; i++) {
            conceptPartitions.add(new UnifiedSet<>());
        }
        int counter = 0;
        int partitionIndex;

        while (occurringConceptsInDataset.hasNext()) {
            ELConcept concept = occurringConceptsInDataset.next();
            partitionIndex = counter % numberOfPartitions;
            conceptPartitions.get(partitionIndex).add(concept);
            counter++;
        }

        List<PartitionModel> partitionModels = new ArrayList<>(numberOfPartitions);

        for (Set<ELConcept> conceptPartition : conceptPartitions) {
            Collection<OWLELRule> rules = OWL2ELSaturationUtils.getOWL2ELRules(elOntology);
            Dataset ontologyFragment = getDatasetFragmentForPartition(conceptPartition);
            PartitionModel pm = new PartitionModel<>(
                    rules,
                    conceptPartition,
                    ontologyFragment
            );
            partitionModels.add(pm);
        }
        return partitionModels;
    }

    @Override
    public boolean isRelevantAxiomToPartition(SaturationPartition<ELConceptInclusion, ELConcept> partition, ELConceptInclusion axiom) {
        Set<ELConcept> conceptPartition = partition.getTermPartition();
        return isRelevantAxiomToPartition(conceptPartition, axiom);
    }

    private boolean isRelevantAxiomToPartition(Set<ELConcept> conceptPartition, ELConceptInclusion axiom) {
        if (conceptPartition.contains(axiom.getSubConcept())
                || conceptPartition.contains(axiom.getSuperConcept())) {
            return true;
        } else if (axiom.getSuperConcept() instanceof ELConceptExistentialRestriction) {
            ELConceptExistentialRestriction exist = (ELConceptExistentialRestriction) axiom.getSuperConcept();
            if (conceptPartition.contains(exist.getFiller())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean checkIfOtherPartitionsRequireAxiom(SaturationPartition<ELConceptInclusion, ELConcept> partition, ELConceptInclusion axiom) {
        Set<ELConcept> conceptPartition = partition.getTermPartition();
        if (!conceptPartition.contains(axiom.getSubConcept())
                || !conceptPartition.contains(axiom.getSuperConcept())) {
            return true;
        } else if (axiom.getSuperConcept() instanceof ELConceptExistentialRestriction) {
            ELConceptExistentialRestriction exist = (ELConceptExistentialRestriction) axiom.getSuperConcept();
            return !conceptPartition.contains(exist.getFiller());
        }
        return false;
    }

     */

}
