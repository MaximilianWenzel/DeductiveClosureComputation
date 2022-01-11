package reasoning.saturation;

import data.Closure;
import networking.ServerData;
import reasoning.rules.Rule;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class SaturationInitializationFactory<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    public abstract List<WorkerModel<C, A, T>> getWorkerModels();

    public List<DistributedWorkerModel<C, A, T>> getDistributedWorkerModels(List<ServerData> serverData) {
        List<WorkerModel<C, A, T>> workerModels = getWorkerModels();
        if (workerModels.size() != serverData.size()) {
            throw new IllegalArgumentException("# workers: " + workerModels.size() + ", # server data: " + serverData.size());
        }
        List<DistributedWorkerModel<C, A, T>> distributedWorkerModels = new ArrayList<>();

        for (int i = 0; i < workerModels.size(); i++) {
            distributedWorkerModels.add(new DistributedWorkerModel<>(workerModels.get(i), serverData.get(i)));
        }
        return distributedWorkerModels;
    }


    public abstract List<? extends A> getInitialAxioms();
    public abstract C getNewClosure();
    public abstract WorkloadDistributor<C, A, T> getWorkloadDistributor();
    public abstract List<Rule<C, A>> generateRules();
    public abstract void resetFactory();
}
