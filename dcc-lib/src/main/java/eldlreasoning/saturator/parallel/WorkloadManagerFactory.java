package eldlreasoning.saturator.parallel;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptInclusion;
import eldlsyntax.IndexedELOntology;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import util.DistributedOWL2ELSaturationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class WorkloadManagerFactory {

    private IndexedELOntology elOntology;
    private Set<ELConcept> usedConceptsInOntology;
    private List<SaturatorPartition> partitions;
    private int numberOfPartitions;
    private BlockingQueue<ELConceptInclusion> initialToDo = new LinkedBlockingQueue<>();
    private WorkloadManager workloadManager;

    public WorkloadManagerFactory(IndexedELOntology elOntology, int numberOfPartitions) {
        this.elOntology = elOntology;
        this.numberOfPartitions = numberOfPartitions;
    }

    public WorkloadManager generate() {
        workloadManager = new WorkloadManager();
        init();
        return workloadManager;
    }

    private void init() {
        this.elOntology.tBox().forEach(elAxiom -> {
            if (elAxiom instanceof ELConceptInclusion) {
                initialToDo.add((ELConceptInclusion) elAxiom);
            }
        });
        this.workloadManager.setToDo(initialToDo);
        this.usedConceptsInOntology = elOntology.getAllUsedConceptsInOntology();
        initPartitions();
    }


    private void initPartitions() {
        // create concept partitions
        Set<ELConcept>[] conceptPartitions = new Set[numberOfPartitions];
        for (int i = 0; i < conceptPartitions.length; i++) {
            conceptPartitions[i] = new UnifiedSet<>();
        }
        int counter = 0;
        int partitionIndex;
        for (ELConcept concept : usedConceptsInOntology) {
            partitionIndex = counter % numberOfPartitions;
            conceptPartitions[partitionIndex].add(concept);
            counter++;
        }

        // init saturator partitions
        this.partitions = new ArrayList<>(numberOfPartitions);
        for (Set<ELConcept> conceptPartition : conceptPartitions) {
            partitions.add(initAndGetPartition(conceptPartition));
        }
        workloadManager.setPartitions(partitions);
    }

    private SaturatorPartition initAndGetPartition(Set<ELConcept> conceptPartition) {
        IndexedELOntology ontologyFragment = getOntologyFragmentForConceptPartition(conceptPartition);

        Set<ELConceptInclusion> partitionClosure = new UnifiedSet<>();
        BlockingQueue<ELConceptInclusion> partitionToDo = new LinkedBlockingQueue<>();

        return new SaturatorPartition(conceptPartition, ontologyFragment, partitionClosure, partitionToDo, workloadManager);
    }


    private IndexedELOntology getOntologyFragmentForConceptPartition(Set<ELConcept> conceptPartition) {
        IndexedELOntology ontologyFragment = new IndexedELOntology();
        for (ELConceptInclusion axiom : elOntology.getOntologyAxioms()) {
            if (DistributedOWL2ELSaturationUtils.isRelevantAxiomToPartition(conceptPartition, axiom)) {
                ontologyFragment.add(axiom);
            }
        }
        return ontologyFragment;
    }

}
