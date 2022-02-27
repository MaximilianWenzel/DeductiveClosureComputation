package benchmark.transitiveclosure;

import benchmark.graphgeneration.GraphGenerator;
import org.roaringbitmap.RoaringBitmap;
import reasoning.rules.Rule;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReachabilitySaturationInitializationFactory extends SaturationInitializationFactory<ReachabilityClosure, Reachability> {

    private int numberOfWorkers;
    private int ruleDelayInNanoSec;
    private List<WorkerModel<ReachabilityClosure, Reachability>> workerModels;
    private GraphGenerator<ToldReachability> graphGenerator;

    public ReachabilitySaturationInitializationFactory(GraphGenerator<ToldReachability> graphGenerator, int numberOfWorkers, int ruleDelayInNanoSec) {
        this.graphGenerator = graphGenerator;
        this.numberOfWorkers = numberOfWorkers;
        this.ruleDelayInNanoSec = ruleDelayInNanoSec;
    }

    @Override
    public List<WorkerModel<ReachabilityClosure, Reachability>> getWorkerModels() {
        if (workerModels != null) {
            return workerModels;
        }

        List<WorkerModel<ReachabilityClosure, Reachability>> workers = new ArrayList<>();
        for (int i = 0; i < numberOfWorkers; i++) {
            WorkerModel<ReachabilityClosure, Reachability> workerModel = new WorkerModel<>(
                    i + 1,
                    getNewClosure(),
                    generateRules()
            );
            workers.add(workerModel);
        }
        workerModels = workers;
        return workerModels;
    }

    @Override
    public Iterator<? extends Reachability> getInitialAxioms() {
        return graphGenerator.generateGraph().iterator();
    }

    @Override
    public ReachabilityClosure getNewClosure() {
        return new ReachabilityClosure();
    }

    @Override
    public WorkloadDistributor<ReachabilityClosure, Reachability> getWorkloadDistributor() {
        return new ReachabilityWorkloadDistributor(numberOfWorkers);
    }

    @Override
    public List<Rule<ReachabilityClosure, Reachability>> generateRules() {
        List<Rule<ReachabilityClosure, Reachability>> reachabilityRules = new ArrayList<>();
        reachabilityRules.add(new InitRule());
        reachabilityRules.add(new DeriveReachabilityRule(ruleDelayInNanoSec));

        return reachabilityRules;
    }

    @Override
    public void resetFactory() {
        workerModels = null;
    }
}
