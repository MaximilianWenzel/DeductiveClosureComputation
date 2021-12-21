package benchmark.echoclosure;

import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.Collections;
import java.util.List;

public class EchoWorkloadDistributor extends WorkloadDistributor<EchoClosure, EchoAxiom, Integer> {

    protected EchoWorkloadDistributor() {

    }

    public EchoWorkloadDistributor(
            List<? extends WorkerModel<EchoClosure, EchoAxiom, Integer>> workerModels) {
        super(workerModels);
    }

    @Override
    public List<Long> getRelevantWorkerIDsForAxiom(EchoAxiom axiom) {
        int partitionID = (axiom.getX() % workerModels.size());
        return Collections.singletonList(workerModels.get(partitionID).getID());
    }
}
