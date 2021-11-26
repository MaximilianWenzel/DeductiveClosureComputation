package reasoning.saturation.distributed;

import reasoning.saturation.workloaddistribution.WorkloadDistributor;

import java.util.Collection;
import java.util.List;

public class OWLELWorkloadDistributor implements WorkloadDistributor {

    @Override
    public List<Long> getRelevantPartitionsForAxiom(Object axiom) {
        /*
        if (consideredAxioms.add(axiom)) {
            for (SaturationPartition partition : partitions) {
                if (isRelevantAxiomToPartition(partition, axiom)) {
                    partition.getToDo().add(axiom);
                }
            }
        }

         */
        return null;
    }
}
