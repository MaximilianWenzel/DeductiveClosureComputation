package eldlreasoning;

import data.DefaultClosure;
import data.IndexedELOntology;
import eldlreasoning.rules.OWLELRule;
import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptInclusion;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.models.WorkerFactory;
import reasoning.saturation.models.WorkerModel;
import util.OWL2ELSaturationUtils;

import java.util.*;

public class OWLELWorkerFactory implements WorkerFactory<DefaultClosure<ELConceptInclusion>, ELConceptInclusion> {

    private final IndexedELOntology elOntology;
    private final int numberOfPartitions;

    public OWLELWorkerFactory(IndexedELOntology elOntology, int numberOfPartitions) {
        this.elOntology = elOntology;
        this.numberOfPartitions = numberOfPartitions;
    }

    @Override
    public List<WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion>> generateWorkers() {
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

        List<WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion>> workers = new ArrayList<>(numberOfPartitions);

        for (Set<ELConcept> conceptPartition : conceptPartitions) {
            Collection<OWLELRule> rules = OWL2ELSaturationUtils.getOWL2ELRules(elOntology);
            WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion> pm = new WorkerModel<>(
                    rules,
                    conceptPartition
            );
            workers.add(pm);
        }
        return workers;
    }

}
