package reasoning.saturation.models;

import data.Closure;

import java.io.Serializable;
import java.util.Collection;

public interface DistributedWorkerFactory<C extends Closure<A>, A extends Serializable> {

    Collection<DistributedWorkerModel<C, A>> generateDistributedWorkers();
}
