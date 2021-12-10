package benchmark.transitiveclosure;

import org.roaringbitmap.RoaringBitmap;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.List;

public class ReachabilitySaturationInitializationFactory extends SaturationInitializationFactory<ReachabilityClosure, Reachability, RoaringBitmap> {

    ReachabilityWorkerFactory workerFactory;
    List<? extends Reachability> initialAxioms;
    int numberOfWorkers;
    ReachabilityWorkloadDistributor workloadDistributor;
    List<WorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> workerModels;

    public ReachabilitySaturationInitializationFactory(List<? extends Reachability> initialAxioms, int numberOfWorkers) {
        this.initialAxioms = initialAxioms;
        this.numberOfWorkers = numberOfWorkers;
        workerFactory = new ReachabilityWorkerFactory(initialAxioms, numberOfWorkers);
        workerModels = workerFactory.generateWorkers();
        workloadDistributor = new ReachabilityWorkloadDistributor(workerModels);
    }

    @Override
    public List<WorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> getWorkerModels() {
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
        return workloadDistributor;
    }
}
