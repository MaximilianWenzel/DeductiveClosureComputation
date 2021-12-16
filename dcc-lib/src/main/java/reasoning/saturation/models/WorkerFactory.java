package reasoning.saturation.models;

import data.Closure;

import java.io.Serializable;
import java.util.Collection;

public interface WorkerFactory<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    Collection<WorkerModel<C, A, T>> generateWorkers();

}