package benchmark.echoclosure;

import reasoning.rules.Rule;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EchoSaturationInitializationFactory
        extends SaturationInitializationFactory<EchoClosure, EchoAxiom> {

    private List<EchoAxiom> initialAxioms;
    private int numberOfWorkers;
    private int numberOfInitialAxioms;
    private List<WorkerModel<EchoClosure, EchoAxiom>> workerModels;

    public EchoSaturationInitializationFactory(int numberOfWorkers, int numberOfInitialAxioms) {
        this.numberOfWorkers = numberOfWorkers;
        this.numberOfInitialAxioms = numberOfInitialAxioms;
        init();
    }

    private void init() {
        initialAxioms = EchoAxiomGenerator.getInitialAxioms(numberOfInitialAxioms);
    }

    @Override
    public List<WorkerModel<EchoClosure, EchoAxiom>> getWorkerModels() {
        if (workerModels != null) {
            return workerModels;
        }
        List<WorkerModel<EchoClosure, EchoAxiom>> workerModels = new ArrayList<>();
        for (int i = 0; i < numberOfWorkers; i++) {
            WorkerModel<EchoClosure, EchoAxiom> workerModel = new WorkerModel<>(
                    i + 1,
                    getNewClosure(),
                    generateRules()
            );
            workerModels.add(workerModel);
        }
        this.workerModels = workerModels;
        return workerModels;
    }

    @Override
    public Iterator<EchoAxiom> getInitialAxioms() {
        return initialAxioms.iterator();
    }

    @Override
    public EchoClosure getNewClosure() {
        return new EchoClosure();
    }

    @Override
    public WorkloadDistributor<EchoClosure, EchoAxiom> getWorkloadDistributor() {
        return new EchoWorkloadDistributor(numberOfWorkers);
    }

    @Override
    public List<Rule<EchoClosure, EchoAxiom>> generateRules() {
        List<Rule<EchoClosure, EchoAxiom>> rules = new ArrayList<>();
        rules.add(new EchoAToBRule());
        rules.add(new EchoBToARule());
        return rules;
    }

    @Override
    public void resetFactory() {
        workerModels = null;
    }


}
