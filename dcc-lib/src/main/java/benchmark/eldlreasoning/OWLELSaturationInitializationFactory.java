package benchmark.eldlreasoning;

import benchmark.eldlreasoning.rules.*;
import data.DefaultClosure;
import data.IndexedELOntology;
import eldlsyntax.ELConceptInclusion;
import reasoning.rules.Rule;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OWLELSaturationInitializationFactory extends
        SaturationInitializationFactory<DefaultClosure<ELConceptInclusion>, ELConceptInclusion> {

    private final IndexedELOntology elOntology;
    private final int numberOfWorkers;
    private List<WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion>> workerModels = null;

    public OWLELSaturationInitializationFactory(IndexedELOntology elOntology, int numberOfWorkers) {
        this.elOntology = elOntology;
        this.numberOfWorkers = numberOfWorkers;
    }

    @Override
    public List<WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion>> getWorkerModels() {
        if (workerModels != null) {
            return workerModels;
        }
        List<WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion>> workers = new ArrayList<>(
                numberOfWorkers);

        for (int i  = 0; i < numberOfWorkers; i++) {
            List<Rule<DefaultClosure<ELConceptInclusion>, ELConceptInclusion>> rules = generateRules();
            WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion> pm = new WorkerModel<>(
                    i + 1,
                    new DefaultClosure<>(),
                    rules
            );
            workers.add(pm);
        }
        workerModels = workers;
        return workerModels;
    }

    @Override
    public Iterator<ELConceptInclusion> getInitialAxioms() {
        return this.elOntology.getInitialAxioms().iterator();
    }

    @Override
    public DefaultClosure<ELConceptInclusion> getNewClosure() {
        return new DefaultClosure<>();
    }

    @Override
    public WorkloadDistributor<DefaultClosure<ELConceptInclusion>, ELConceptInclusion> getWorkloadDistributor() {
        return new OWLELWorkloadDistributor(numberOfWorkers);
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
