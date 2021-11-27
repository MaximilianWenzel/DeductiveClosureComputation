package eldlreasoning;

import data.Dataset;
import data.IndexedELOntology;
import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptExistentialRestriction;
import eldlsyntax.ELConceptInclusion;
import reasoning.saturation.models.PartitionModel;
import reasoning.saturation.parallel.ParallelSaturation;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.*;

public class OWL2ELParallelSaturation extends ParallelSaturation {
    protected OWL2ELParallelSaturation(List<Object> initialAxioms, Collection<PartitionModel> partitionModels, WorkloadDistributor workloadDistributor) {
        super(initialAxioms, partitionModels, workloadDistributor);
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
