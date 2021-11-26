package reasoning.saturation.distributed;

import reasoning.saturation.models.PartitionModel;

import java.util.Collection;

public interface PartitionFactory {

    Collection<PartitionModel> generatePartitions();
}
