package benchmark.transitiveclosure;

import org.roaringbitmap.RoaringBitmap;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ReachabilityWorkloadDistributor extends WorkloadDistributor<ReachabilityClosure, Reachability, RoaringBitmap> {


    protected ReachabilityWorkloadDistributor() {

    }

    public ReachabilityWorkloadDistributor(List<? extends WorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> workerModels) {
        super(workerModels);
    }

    @Override
    public Stream<Long> getRelevantWorkerIDsForAxiom(Reachability axiom) {
        Stream.Builder<Long> workerIDs = Stream.builder();
        workerModels.forEach(worker -> {
            if (isRelevantAxiomToWorker(worker, axiom)) {
                workerIDs.add(worker.getID());
            }
        });
        return workerIDs.build();
    }

    public boolean isRelevantAxiomToWorker(WorkerModel<ReachabilityClosure, Reachability, RoaringBitmap> worker, Reachability axiom) {
        RoaringBitmap responsibleNodeIDs = worker.getWorkerTerms();
        if (responsibleNodeIDs.contains(axiom.getSourceNode())
                || responsibleNodeIDs.contains(axiom.getDestinationNode())) {
            return true;
        }
        return false;
    }

}
