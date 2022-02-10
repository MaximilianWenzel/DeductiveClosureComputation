package benchmark.transitiveclosure;

import reasoning.saturation.workload.WorkloadDistributor;

import java.util.stream.Stream;

public class ReachabilityWorkloadDistributor
        extends WorkloadDistributor<ReachabilityClosure, Reachability> {


    private int numberOfWorkers;

    protected ReachabilityWorkloadDistributor() {

    }

    public ReachabilityWorkloadDistributor(int numberOfWorkers) {
        super();
        this.numberOfWorkers = numberOfWorkers;
    }

    @Override
    public Stream<Long> getRelevantWorkerIDsForAxiom(Reachability axiom) {
        if (axiom instanceof ToldReachability) {
            return Stream.of((long) (axiom.getSourceNode() % numberOfWorkers + 1));
        } else if (axiom instanceof DerivedReachability) {
            return Stream.of((long) (axiom.getDestinationNode() % numberOfWorkers + 1));
        }
        return Stream.empty();
    }


}
