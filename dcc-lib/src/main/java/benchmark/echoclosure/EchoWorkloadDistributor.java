package benchmark.echoclosure;

import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class EchoWorkloadDistributor extends WorkloadDistributor<EchoClosure, EchoAxiom, Integer> {

    protected EchoWorkloadDistributor() {

    }

    public EchoWorkloadDistributor(
            List<? extends WorkerModel<EchoClosure, EchoAxiom, Integer>> workerModels) {
        super(workerModels);
    }

    @Override
    public Stream<Long> getRelevantWorkerIDsForAxiom(EchoAxiom axiom) {
        int partitionID = (axiom.getX() % workerModels.size());
        return Stream.of(workerModels.get(partitionID).getID());
    }
}
