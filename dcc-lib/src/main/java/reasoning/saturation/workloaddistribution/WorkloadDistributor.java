package reasoning.saturation.workloaddistribution;

import java.util.List;

public interface WorkloadDistributor {
    List<Long> getRelevantPartitionsForAxiom(Object axiom);
}
