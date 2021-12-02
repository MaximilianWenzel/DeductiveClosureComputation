package reasoning.saturation.models;

import java.util.Collection;

public interface PartitionFactory {

    Collection<WorkerModel> generatePartitions();

}
