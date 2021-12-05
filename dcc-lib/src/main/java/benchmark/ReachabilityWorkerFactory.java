package benchmark;

import networking.ServerData;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.roaringbitmap.longlong.Roaring64NavigableMap;
import reasoning.rules.Rule;
import reasoning.saturation.models.DistributedWorkerFactory;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.models.WorkerFactory;
import reasoning.saturation.models.WorkerModel;

import java.util.*;

public class ReachabilityWorkerFactory implements DistributedWorkerFactory<ReachabilityClosure, Reachability>,
        WorkerFactory<ReachabilityClosure, Reachability> {

    private List<Reachability> initialAxioms;
    private Roaring64NavigableMap nodeIDs = new Roaring64NavigableMap();
    private int numberOfWorkers;
    private List<ServerData> workerServerData;

    public ReachabilityWorkerFactory(List<Reachability> initialAxioms, List<ServerData> workerServerData) {
        this.initialAxioms = initialAxioms;
        this.numberOfWorkers = workerServerData.size();
        this.workerServerData = workerServerData;
        init();
    }

    private void init() {
        for (Reachability r : initialAxioms) {
            nodeIDs.add(r.getSourceNode());
            nodeIDs.add(r.getDestinationNode());
        }
    }

    @Override
    public Collection<DistributedWorkerModel<ReachabilityClosure, Reachability>> generateDistributedWorkers() {
        List<WorkerModel<ReachabilityClosure, Reachability>> workers = generateWorkers();

        List<DistributedWorkerModel<ReachabilityClosure, Reachability>> distributedWorkerModels = new ArrayList<>();

        for (int i = 0; i < workers.size(); i++) {
            WorkerModel<ReachabilityClosure, Reachability> worker = workers.get(i);
            distributedWorkerModels.add(new DistributedWorkerModel<>(
                    worker.getRules(),
                    worker.getWorkerTerms(),
                    workerServerData.get(i)
            ));
        }
        return distributedWorkerModels;
    }

    @Override
    public List<WorkerModel<ReachabilityClosure, Reachability>> generateWorkers() {
        List<Set<Long>> nodeIDsForWorkers = new ArrayList<>(this.numberOfWorkers);
        for (int i = 0; i < numberOfWorkers; i++) {
            nodeIDsForWorkers.add(new UnifiedSet<>());
        }

        Iterator<Long> nodeIDIterator = nodeIDs.iterator();
        int i = 0;
        while (nodeIDIterator.hasNext()) {
            nodeIDsForWorkers.get(i % numberOfWorkers).add(nodeIDIterator.next());
            i++;
        }

        List<WorkerModel<ReachabilityClosure, Reachability>> workers = new ArrayList<>();
        for (Set<Long> nodeIDsForWorker : nodeIDsForWorkers) {
            WorkerModel<ReachabilityClosure, Reachability> workerModel = new WorkerModel<>(
                    getReachabilityRules(),
                    nodeIDsForWorker
            );
            workers.add(workerModel);
        }
        return workers;
    }

    public static Collection<Rule<ReachabilityClosure, Reachability>> getReachabilityRules() {
        List<Rule<ReachabilityClosure, Reachability>> reachabilityRules = new ArrayList<>();
        reachabilityRules.add(new InitRule());
        reachabilityRules.add(new DeriveReachabilityRule());
        return reachabilityRules;
    }
}
