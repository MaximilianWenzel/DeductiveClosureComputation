package reasoning.saturator.distributed;

public class OWLELWorkloadDistributor implements WorkloadDistributor {

    @Override
    public boolean distributeToPartitions(Object axiom) {
        /*
        if (consideredAxioms.add(axiom)) {
            for (SaturationPartition partition : partitions) {
                if (isRelevantAxiomToPartition(partition, axiom)) {
                    partition.getToDo().add(axiom);
                }
            }
        }

         */
        return false;
    }
}
