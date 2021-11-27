package reasoning.saturation.models;

import data.IndexedELOntology;
import eldlreasoning.rules.OWLELRule;
import eldlsyntax.ELConcept;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import util.OWL2ELSaturationUtils;

import java.util.*;

public class OWLELPartitionFactory implements PartitionFactory {

    private final IndexedELOntology elOntology;
    private final int numberOfPartitions;

    public OWLELPartitionFactory(IndexedELOntology elOntology, int numberOfPartitions) {
        this.elOntology = elOntology;
        this.numberOfPartitions = numberOfPartitions;
    }

    @Override
    public List<PartitionModel> generatePartitions() {
        Iterator<ELConcept> occurringConceptsInDataset = this.elOntology.getAllUsedConceptsInOntology().iterator();

        // partition by concepts
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
            PartitionModel pm = new PartitionModel(
                    rules,
                    conceptPartition
            );
            partitionModels.add(pm);
        }
        return partitionModels;
    }

}
