package reasoning.saturation.models;

import data.Closure;

import java.io.Serializable;
import java.util.List;

public interface DistributedWorkerFactory<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    List<DistributedWorkerModel<C, A, T>> generateDistributedWorkers();
}
