package reasoning.saturation.models;

import java.util.Collection;

public interface DistributedPartitionFactory {

    Collection<DistributedWorkerModel> generateDistributedPartitions();
}
