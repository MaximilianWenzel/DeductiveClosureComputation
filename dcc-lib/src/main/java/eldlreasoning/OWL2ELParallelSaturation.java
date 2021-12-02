package eldlreasoning;

import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.parallel.ParallelSaturation;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.*;

public class OWL2ELParallelSaturation extends ParallelSaturation {
    protected OWL2ELParallelSaturation(List<? extends Serializable> initialAxioms, Collection<WorkerModel> workerModels, WorkloadDistributor workloadDistributor) {
        super(initialAxioms, workerModels, workloadDistributor);
    }
/*
    private int numberOfPartitions;
    private IndexedELOntology elOntology;

    public OWL2ELParallelSaturation(IndexedELOntology elOntology, int numberOfPartitions) {
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
    public boolean isRelevantAxiomToPartition(SaturationPartition<ELConceptInclusion, ELConcept> partition, ELConceptInclusion axiom) {
        Set<ELConcept> conceptPartition = partition.getTermPartition();
        return isRelevantAxiomToPartition(conceptPartition, axiom);
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
