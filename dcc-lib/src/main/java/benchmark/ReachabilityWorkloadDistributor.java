package benchmark;

import org.roaringbitmap.RoaringBitmap;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.List;

public class ReachabilityWorkloadDistributor extends WorkloadDistributor<ReachabilityClosure, Reachability, RoaringBitmap> {

    public ReachabilityWorkloadDistributor(List<? extends WorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> workerModels) {
        super(workerModels);
    }

    @Override
    public List<Long> getRelevantWorkerIDsForAxiom(Reachability axiom) {
        List<Long> result = new ArrayList<>();
        workerModels.forEach(worker -> {
            if (isRelevantAxiomToWorker(worker, axiom)) {
                result.add(worker.getID());
            }
        });
        return result;
    }

    @Override
    public boolean isRelevantAxiomToWorker(WorkerModel<ReachabilityClosure, Reachability, RoaringBitmap> worker, Reachability axiom) {
        RoaringBitmap responsibleNodeIDs = worker.getWorkerTerms();
        if (responsibleNodeIDs.contains(axiom.getSourceNode())
                || responsibleNodeIDs.contains(axiom.getDestinationNode())) {
            return true;
        }
        return false;
    }

}
