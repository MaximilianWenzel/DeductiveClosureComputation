package eldlreasoning;

import data.Dataset;
import data.IndexedELOntology;
import eldlreasoning.rules.OWLELRule;
import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptExistentialRestriction;
import eldlsyntax.ELConceptInclusion;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturator.parallel.ParallelSaturation;
import reasoning.saturator.PartitionModel;
import reasoning.saturator.parallel.SaturationPartition;
import util.OWL2ELSaturationUtils;

import java.util.*;

public class OWL2ELParallelSaturation extends ParallelSaturation<ELConceptInclusion, ELConcept> {

    private int numberOfPartitions;
    private IndexedELOntology elOntology;

    public OWL2ELParallelSaturation(IndexedELOntology elOntology, int numberOfPartitions) {
        super(elOntology);
        this.elOntology = elOntology;
        this.numberOfPartitions = numberOfPartitions;
    }

    protected Dataset<ELConceptInclusion, ELConcept> getDatasetFragmentForPartition(Set<ELConcept> termPartition) {
        IndexedELOntology ontologyFragment = elOntology.getOntologyWithIndexedNegativeConcepts();
        for (ELConceptInclusion axiom : elOntology.getOntologyAxioms()) {
            if (this.isRelevantAxiomToPartition(termPartition, axiom)) {
                ontologyFragment.add(axiom);
            }
        }
        return ontologyFragment;
    }

    @Override
    protected List<PartitionModel<ELConceptInclusion, ELConcept>> initializePartitions() {

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

        List<PartitionModel<ELConceptInclusion, ELConcept>> partitionModels = new ArrayList<>(numberOfPartitions);

        for (Set<ELConcept> conceptPartition : conceptPartitions) {
            Collection<OWLELRule> rules = OWL2ELSaturationUtils.getOWL2ELRules(elOntology);
            Dataset<ELConceptInclusion, ELConcept> ontologyFragment = getDatasetFragmentForPartition(conceptPartition);
            PartitionModel<ELConceptInclusion, ELConcept> pm = new PartitionModel<>(
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

}
