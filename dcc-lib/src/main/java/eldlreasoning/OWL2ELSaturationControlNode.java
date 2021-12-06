package eldlreasoning;

import data.Closure;
import reasoning.saturation.distributed.SaturationControlNode;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.*;

public class OWL2ELSaturationControlNode extends SaturationControlNode {
    protected OWL2ELSaturationControlNode(List workers, WorkloadDistributor workloadDistributor, List initialAxioms, Closure resultingClosure) {
        super(workers, workloadDistributor, initialAxioms, resultingClosure);
    }


    /*
    private int numberOfWorkers;
    private IndexedELOntology elOntology;

    public OWL2ELSaturationControlNode(IndexedELOntology elOntology, int numberOfWorkers) {
        super(elOntology);
        this.elOntology = elOntology;
        this.numberOfWorkers = numberOfWorkers;
    }

    protected Dataset getDatasetFragmentForWorker(Set<ELConcept> termWorker) {
        IndexedELOntology ontologyFragment = elOntology.getOntologyWithIndexedNegativeConcepts();
        for (ELConceptInclusion axiom : elOntology.getOntologyAxioms()) {
            if (this.isRelevantAxiomToWorker(termWorker, axiom)) {
                ontologyFragment.add(axiom);
            }
        }
        return ontologyFragment;
    }

    @Override
    protected List<WorkerModel> initializeWorkers() {

        Iterator<ELConcept> occurringConceptsInDataset = this.elOntology.getAllUsedConceptsInOntology().iterator();

        // create concept workers
        List<Set<ELConcept>> conceptWorkers = new ArrayList<>(numberOfWorkers);
        for (int i = 0; i < numberOfWorkers; i++) {
            conceptWorkers.add(new UnifiedSet<>());
        }
        int counter = 0;
        int workerIndex;

        while (occurringConceptsInDataset.hasNext()) {
            ELConcept concept = occurringConceptsInDataset.next();
            workerIndex = counter % numberOfWorkers;
            conceptWorkers.get(workerIndex).add(concept);
            counter++;
        }

        List<WorkerModel> workerModels = new ArrayList<>(numberOfWorkers);

        for (Set<ELConcept> conceptWorker : conceptWorkers) {
            Collection<OWLELRule> rules = OWL2ELSaturationUtils.getOWL2ELRules(elOntology);
            Dataset ontologyFragment = getDatasetFragmentForWorker(conceptWorker);
            WorkerModel pm = new WorkerModel<>(
                    rules,
                    conceptWorker,
                    ontologyFragment
            );
            workerModels.add(pm);
        }
        return workerModels;
    }

    @Override
    public boolean isRelevantAxiomToWorker(SaturationWorker<ELConceptInclusion, ELConcept> worker, ELConceptInclusion axiom) {
        Set<ELConcept> conceptWorker = worker.getTermWorker();
        return isRelevantAxiomToWorker(conceptWorker, axiom);
    }

    private boolean isRelevantAxiomToWorker(Set<ELConcept> conceptWorker, ELConceptInclusion axiom) {
        if (conceptWorker.contains(axiom.getSubConcept())
                || conceptWorker.contains(axiom.getSuperConcept())) {
            return true;
        } else if (axiom.getSuperConcept() instanceof ELConceptExistentialRestriction) {
            ELConceptExistentialRestriction exist = (ELConceptExistentialRestriction) axiom.getSuperConcept();
            if (conceptWorker.contains(exist.getFiller())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean checkIfOtherWorkersRequireAxiom(SaturationWorker<ELConceptInclusion, ELConcept> worker, ELConceptInclusion axiom) {
        Set<ELConcept> conceptWorker = worker.getTermWorker();
        if (!conceptWorker.contains(axiom.getSubConcept())
                || !conceptWorker.contains(axiom.getSuperConcept())) {
            return true;
        } else if (axiom.getSuperConcept() instanceof ELConceptExistentialRestriction) {
            ELConceptExistentialRestriction exist = (ELConceptExistentialRestriction) axiom.getSuperConcept();
            return !conceptWorker.contains(exist.getFiller());
        }
        return false;
    }

     */

}
