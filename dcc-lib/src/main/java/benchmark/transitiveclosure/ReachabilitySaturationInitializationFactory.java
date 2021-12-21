package benchmark.transitiveclosure;

import org.roaringbitmap.RoaringBitmap;
import reasoning.rules.Rule;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReachabilitySaturationInitializationFactory extends SaturationInitializationFactory<ReachabilityClosure, Reachability, RoaringBitmap> {

    private List<? extends Reachability> initialAxioms;
    private int numberOfWorkers;
    private int ruleDelayInNanoSec;
    private List<WorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> workerModels;

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
    public List<WorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> getWorkerModels() {
        if (workerModels != null) {
            return workerModels;
        }

        List<RoaringBitmap> nodeIDsForWorkers = new ArrayList<>(this.numberOfWorkers);
        for (int i = 0; i < numberOfWorkers; i++) {
            nodeIDsForWorkers.add(new RoaringBitmap());
        }

        Iterator<Integer> nodeIDIterator = nodeIDs.iterator();
        int i = 0;
        while (nodeIDIterator.hasNext()) {
            nodeIDsForWorkers.get(i % numberOfWorkers).add(nodeIDIterator.next());
            i++;
        }

        List<WorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> workers = new ArrayList<>();
        for (RoaringBitmap nodeIDsForWorker : nodeIDsForWorkers) {
            WorkerModel<ReachabilityClosure, Reachability, RoaringBitmap> workerModel = new WorkerModel<>(
                    generateRules(),
                    nodeIDsForWorker
            );
            workers.add(workerModel);
        }
        workerModels = workers;
        return workerModels;
    }

    @Override
    public List<? extends Reachability> getInitialAxioms() {
        return initialAxioms;
    }

    @Override
    public ReachabilityClosure getNewClosure() {
        return new ReachabilityClosure();
    }

    @Override
    public WorkloadDistributor<ReachabilityClosure, Reachability, RoaringBitmap> getWorkloadDistributor() {
        return new ReachabilityWorkloadDistributor(getWorkerModels());
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
