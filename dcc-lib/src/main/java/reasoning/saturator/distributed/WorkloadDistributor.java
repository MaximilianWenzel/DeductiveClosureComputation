package reasoning.saturator.distributed;

public interface WorkloadDistributor {
    boolean distributeToPartitions(Object axiom);
}
