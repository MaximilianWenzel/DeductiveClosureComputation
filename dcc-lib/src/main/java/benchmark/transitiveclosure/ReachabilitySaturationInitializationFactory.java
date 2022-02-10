package benchmark.transitiveclosure;

import org.roaringbitmap.RoaringBitmap;
import reasoning.rules.Rule;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReachabilitySaturationInitializationFactory extends SaturationInitializationFactory<ReachabilityClosure, Reachability> {

    private List<? extends Reachability> initialAxioms;
    private int numberOfWorkers;
    private int ruleDelayInNanoSec;
    private List<WorkerModel<ReachabilityClosure, Reachability>> workerModels;

    private RoaringBitmap nodeIDs = new RoaringBitmap();


    public ReachabilitySaturationInitializationFactory(List<? extends Reachability> initialAxioms, int numberOfWorkers, int ruleDelayInNanoSec) {
        this.initialAxioms = initialAxioms;
        this.numberOfWorkers = numberOfWorkers;
        this.ruleDelayInNanoSec = ruleDelayInNanoSec;
        init();
    }


    private void init() {
        for (Reachability r : initialAxioms) {
            nodeIDs.add(r.getSourceNode());
            nodeIDs.add(r.getDestinationNode());
        }
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
        return initialAxioms.iterator();
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
