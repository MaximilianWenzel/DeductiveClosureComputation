package benchmark.echoclosure;

import reasoning.rules.Rule;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.List;

public class EchoSaturationInitializationFactory
        extends SaturationInitializationFactory<EchoClosure, EchoAxiom, Integer> {

    private List<EchoAxiom> initialAxioms;
    private int numberOfWorkers;
    private int numberOfInitialAxioms;
    private List<WorkerModel<EchoClosure, EchoAxiom, Integer>> workerModels;

    public EchoSaturationInitializationFactory(int numberOfWorkers, int numberOfInitialAxioms) {
        this.numberOfWorkers = numberOfWorkers;
        this.numberOfInitialAxioms = numberOfInitialAxioms;
        init();
    }

    private void init() {
        initialAxioms = EchoAxiomGenerator.getInitialAxioms(numberOfInitialAxioms);
    }

    @Override
    public List<WorkerModel<EchoClosure, EchoAxiom, Integer>> getWorkerModels() {
        if (workerModels != null) {
            return workerModels;
        }
        List<WorkerModel<EchoClosure, EchoAxiom, Integer>> workerModels = new ArrayList<>();
        for (int i = 0; i < numberOfWorkers; i++) {
            WorkerModel<EchoClosure, EchoAxiom, Integer> workerModel = new WorkerModel<>(
                    getNewClosure(),
                    generateRules(),
                    i
            );
            workerModels.add(workerModel);
        }
        this.workerModels = workerModels;
        return workerModels;
    }

    @Override
    public List<? extends EchoAxiom> getInitialAxioms() {
        return initialAxioms;
    }

    @Override
    public EchoClosure getNewClosure() {
        return new EchoClosure();
    }

    @Override
    public WorkloadDistributor<EchoClosure, EchoAxiom, Integer> getWorkloadDistributor() {
        return new EchoWorkloadDistributor(getWorkerModels());
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
