package eldlreasoning.saturator.parallel;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptExistentialRestriction;
import eldlsyntax.ELConceptInclusion;
import eldlsyntax.IndexedELOntology;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class WorkloadManager implements Runnable {
    private IndexedELOntology elOntology;

    private Set<ELConcept> usedConceptsInOntology;

    private List<SaturatorPartition> partitions;
    private int numberOfPartitions;

    private Set<ELConceptInclusion> consideredAxioms = new UnifiedSet<>();
    private BlockingQueue<ELConceptInclusion> toDo = new LinkedBlockingQueue<>();

    private volatile boolean saturationFinished = false;

    public WorkloadManager(IndexedELOntology elOntology, int numberOfPartitions) {
        this.elOntology = elOntology;
        this.numberOfPartitions = numberOfPartitions;
        init();
    }

    private void init() {
        this.elOntology.tBox().forEach(elAxiom -> {
            if (elAxiom instanceof ELConceptInclusion) {
                toDo.add((ELConceptInclusion) elAxiom);
            }
        });
        this.usedConceptsInOntology = elOntology.getAllUsedConceptsInOntology();
        initPartitions();
    }

    public static boolean isRelevantAxiomToPartition(Set<ELConcept> conceptPartition, ELConceptInclusion axiom) {
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

    public static boolean checkIfOtherPartitionsRequireAxiom(Set<ELConcept> conceptPartition, ELConceptInclusion axiom) {
        if (!conceptPartition.contains(axiom.getSubConcept())
                || !conceptPartition.contains(axiom.getSuperConcept())) {
            return true;
        } else if (axiom.getSuperConcept() instanceof ELConceptExistentialRestriction) {
            ELConceptExistentialRestriction exist = (ELConceptExistentialRestriction) axiom.getSuperConcept();
            return !conceptPartition.contains(exist.getFiller());
        }
        return false;
    }

    @Override
    public void run() {
        try {
            while (!saturationFinished) {
                ELConceptInclusion axiom = null;
                axiom = toDo.take();
                distributeAxiom(axiom);
            }
        } catch (InterruptedException e) {
            // thread terminated
        }

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
    }


    private SaturatorPartition initAndGetPartition(Set<ELConcept> conceptPartition) {
        IndexedELOntology ontologyFragment = getOntologyFragmentForConceptPartition(conceptPartition);

        Set<ELConceptInclusion> partitionClosure = new UnifiedSet<>();
        BlockingQueue<ELConceptInclusion> partitionToDo = new LinkedBlockingQueue<>();

        return new SaturatorPartition(conceptPartition, ontologyFragment, partitionClosure, partitionToDo, this);
    }

    private IndexedELOntology getOntologyFragmentForConceptPartition(Set<ELConcept> conceptPartition) {
        IndexedELOntology ontologyFragment = new IndexedELOntology();
        for (ELConceptInclusion axiom : elOntology.getOntologyAxioms()) {
            if (isRelevantAxiomToPartition(conceptPartition, axiom)) {
                ontologyFragment.add(axiom);
            }
        }
        return ontologyFragment;
    }

    private void distributeAxiom(ELConceptInclusion axiom) {
        if (consideredAxioms.add(axiom)) {
            for (SaturatorPartition partition : partitions) {
                if (isRelevantAxiomToPartition(partition.getConceptPartition(), axiom)) {
                    partition.getToDo().add(axiom);
                }
            }
        }
    }

    public boolean isSaturationFinished() {
        return saturationFinished;
    }

    public void setSaturationFinished(boolean saturationFinished) {
        this.saturationFinished = saturationFinished;
    }

    public BlockingQueue<ELConceptInclusion> getToDo() {
        return toDo;
    }

    public List<SaturatorPartition> getPartitions() {
        return partitions;
    }
}
