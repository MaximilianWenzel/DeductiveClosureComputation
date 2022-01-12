package benchmark.eldlreasoning;

import benchmark.eldlreasoning.rules.*;
import data.DefaultClosure;
import data.IndexedELOntology;
import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptInclusion;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.rules.Rule;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OWLELSaturationInitializationFactory extends
        SaturationInitializationFactory<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> {

    private final IndexedELOntology elOntology;
    private final int numberOfWorkers;
    private List<WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>>> workerModels = null;

    public OWLELSaturationInitializationFactory(IndexedELOntology elOntology, int numberOfWorkers) {
        this.elOntology = elOntology;
        this.numberOfWorkers = numberOfWorkers;
    }

    @Override
    public List<WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>>> getWorkerModels() {
        if (workerModels != null) {
            return workerModels;
        }

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

        List<WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>>> workers = new ArrayList<>(
                numberOfWorkers);

        for (UnifiedSet<ELConcept> conceptWorker : conceptWorkers) {
            List<Rule<DefaultClosure<ELConceptInclusion>, ELConceptInclusion>> rules = generateRules();
            WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> pm = new WorkerModel<>(
                    new DefaultClosure<>(),
                    rules,
                    conceptWorker
            );
            workers.add(pm);
        }
        workerModels = workers;
        return workerModels;
    }

    @Override
    public List<? extends ELConceptInclusion> getInitialAxioms() {
        return this.elOntology.getInitialAxioms();
    }

    @Override
    public DefaultClosure<ELConceptInclusion> getNewClosure() {
        return new DefaultClosure<>();
    }

    @Override
    public WorkloadDistributor<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> getWorkloadDistributor() {
        return new OWLELWorkloadDistributor(getWorkerModels());
    }

    @Override
    public List<Rule<DefaultClosure<ELConceptInclusion>, ELConceptInclusion>> generateRules() {
        List<Rule<DefaultClosure<ELConceptInclusion>, ELConceptInclusion>> rules = new ArrayList<>();
        rules.add(new ComposeConjunctionRule(elOntology.getNegativeConcepts()));
        rules.add(new DecomposeConjunctionRule());
        rules.add(new ReflexiveSubsumptionRule());
        rules.add(new SubsumedByTopRule(elOntology.getTop()));
        rules.add(new UnfoldExistentialRule());
        rules.add(new UnfoldSubsumptionRule(elOntology.getOntologyAxioms()));
        return rules;
    }

    @Override
    public void resetFactory() {
        workerModels = null;
    }
}
