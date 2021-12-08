package benchmark;

import networking.ServerData;
import org.roaringbitmap.RoaringBitmap;
import reasoning.rules.Rule;
import reasoning.saturation.models.DistributedWorkerFactory;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.models.WorkerFactory;
import reasoning.saturation.models.WorkerModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ReachabilityWorkerFactory implements DistributedWorkerFactory<ReachabilityClosure, Reachability, RoaringBitmap>,
        WorkerFactory<ReachabilityClosure, Reachability, RoaringBitmap> {

    private List<? extends Reachability> initialAxioms;
    private RoaringBitmap nodeIDs = new RoaringBitmap();
    private int numberOfWorkers;
    private List<ServerData> workerServerData;

    public ReachabilityWorkerFactory(List<? extends Reachability> initialAxioms, List<ServerData> workerServerData) {
        this.initialAxioms = initialAxioms;
        this.numberOfWorkers = workerServerData.size();
        this.workerServerData = workerServerData;
        init();
    }

    public ReachabilityWorkerFactory(List<? extends Reachability> initialAxioms, int numberOfWorkers) {
        this.initialAxioms = initialAxioms;
        this.numberOfWorkers = numberOfWorkers;
        init();
    }

    public static Collection<Rule<ReachabilityClosure, Reachability>> getReachabilityRules() {
        List<Rule<ReachabilityClosure, Reachability>> reachabilityRules = new ArrayList<>();
        reachabilityRules.add(new InitRule());
        reachabilityRules.add(new DeriveReachabilityRule());
        return reachabilityRules;
    }

    private void init() {
        for (Reachability r : initialAxioms) {
            nodeIDs.add(r.getSourceNode());
            nodeIDs.add(r.getDestinationNode());
        }
    }

    @Override
    public List<DistributedWorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> generateDistributedWorkers() {
        List<WorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> workers = generateWorkers();

        List<DistributedWorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> distributedWorkerModels = new ArrayList<>();

        for (int i = 0; i < workers.size(); i++) {
            WorkerModel<ReachabilityClosure, Reachability, RoaringBitmap> worker = workers.get(i);
            distributedWorkerModels.add(new DistributedWorkerModel<>(
                    worker.getRules(),
                    worker.getWorkerTerms(),
                    workerServerData.get(i)
            ));
        }
        return distributedWorkerModels;
    }

    @Override
    public List<WorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> generateWorkers() {
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
                    getReachabilityRules(),
                    nodeIDsForWorker
            );
            workers.add(workerModel);
        }
        return workers;
    }
}
