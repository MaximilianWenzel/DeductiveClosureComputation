package reasoning.saturation;

import data.Closure;
import networking.ServerData;
import reasoning.rules.Rule;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A class that can be used to initialize the computation of the deductive closure. It can be used in order to obtain the internal
 * representation of all deployed workers and get the initial axioms for the specified logic problem.
 *
 * @param <C> Type of the resulting deductive closure.
 * @param <A> Type of the axioms in the resulting deductive closure.
 */
public abstract class SaturationInitializationFactory<C extends Closure<A>, A extends Serializable> {

    public abstract List<WorkerModel<C, A>> getWorkerModels();

    public List<DistributedWorkerModel<C, A>> getDistributedWorkerModels(List<ServerData> serverData) {
        List<WorkerModel<C, A>> workerModels = getWorkerModels();
        if (workerModels.size() != serverData.size()) {
            throw new IllegalArgumentException("# workers: " + workerModels.size() + ", # server data: " + serverData.size());
        }
        List<DistributedWorkerModel<C, A>> distributedWorkerModels = new ArrayList<>();

        for (int i = 0; i < workerModels.size(); i++) {
            distributedWorkerModels.add(new DistributedWorkerModel<>(workerModels.get(i), serverData.get(i)));
        }
        return distributedWorkerModels;
    }


    public abstract Iterator<? extends A> getInitialAxioms();

    public abstract C getNewClosure();

    public abstract WorkloadDistributor<C, A> getWorkloadDistributor();

    public abstract List<Rule<C, A>> generateRules();

    public abstract void resetFactory();
}
