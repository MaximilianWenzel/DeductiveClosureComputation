package benchmark.echoclosure;

import reasoning.saturation.workload.WorkloadDistributor;

import java.util.stream.Stream;

public class EchoWorkloadDistributor extends WorkloadDistributor<EchoClosure, EchoAxiom> {

    private int numberOfWorkers;

    protected EchoWorkloadDistributor() {

    }

    public EchoWorkloadDistributor(int numberOfWorkers) {
        this.numberOfWorkers = numberOfWorkers;
    }

    @Override
    public Stream<Long> getRelevantWorkerIDsForAxiom(EchoAxiom axiom) {
        long workerID = (axiom.getX() % numberOfWorkers) + 1;
        return Stream.of(workerID);
    }
}
