package eldlreasoning;

import data.DefaultClosure;
import data.IndexedELOntology;
import eldlreasoning.rules.*;
import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptInclusion;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.models.WorkerFactory;
import reasoning.saturation.models.WorkerModel;
import util.OWL2ELSaturationUtils;

import java.util.*;

public class OWLELWorkerFactory implements WorkerFactory<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> {

    private final IndexedELOntology elOntology;
    private final int numberOfWorkers;

    public OWLELWorkerFactory(IndexedELOntology elOntology, int numberOfWorkers) {
        this.elOntology = elOntology;
        this.numberOfWorkers = numberOfWorkers;
    }

    @Override
    public List<WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>>> generateWorkers() {
        Iterator<ELConcept> occurringConceptsInDataset = this.elOntology.getAllUsedConceptsInOntology().iterator();

        // worker by concepts
        List<UnifiedSet<ELConcept>> conceptWorkers = new ArrayList<>(numberOfWorkers);
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

        List<WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>>> workers = new ArrayList<>(numberOfWorkers);

        for (UnifiedSet<ELConcept> conceptWorker : conceptWorkers) {
            List<OWLELRule> rules = getOWL2ELRules(elOntology);
            WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> pm = new WorkerModel<>(
                    rules,
                    conceptWorker
            );
            workers.add(pm);
        }
        return workers;
    }

    public static List<OWLELRule> getOWL2ELRules(IndexedELOntology elOntology) {
        List<OWLELRule> rules = new ArrayList<>();
        rules.add(new ComposeConjunctionRule(elOntology.getNegativeConcepts()));
        rules.add(new DecomposeConjunctionRule());
        rules.add(new ReflexiveSubsumptionRule());
        rules.add(new SubsumedByTopRule(elOntology.getTop()));
        rules.add(new UnfoldExistentialRule());
        rules.add(new UnfoldSubsumptionRule(elOntology.getOntologyAxioms()));
        return rules;
    }

}
