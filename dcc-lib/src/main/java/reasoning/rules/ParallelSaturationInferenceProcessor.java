package reasoning.rules;

import data.Closure;
import data.ToDoQueue;
import enums.SaturationStatusMessage;
import reasoning.saturation.Saturation;
import reasoning.saturation.parallel.SaturationContext;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ParallelSaturationInferenceProcessor<C extends Closure<A>, A extends Serializable, T extends Serializable> implements InferenceProcessor<A> {

    private final WorkloadDistributor<C, A, T> distributor;
    private Map<Long, SaturationContext<C, A, T>> workerIDToSaturationContext;

    public ParallelSaturationInferenceProcessor(WorkloadDistributor<C, A, T> distributor, Map<Long, SaturationContext<C, A, T>> workerIDToSaturationContext) {
        this.distributor = distributor;
        this.workerIDToSaturationContext = workerIDToSaturationContext;
    }

    @Override
    public void processInference(A axiom) {
        List<Long> workerIDs = distributor.getRelevantWorkerIDsForAxiom(axiom);
        for (Long workerID : workerIDs) {
            SaturationContext<C, A, T> saturationContext = workerIDToSaturationContext.get(workerID);
            ToDoQueue<A> toDo = saturationContext.getToDo();
            synchronized (toDo) {
                // TODO: optimize lock
                if (saturationContext.isSaturationConverged()) {
                    saturationContext.setSaturationConverged(false);
                    saturationContext.sendStatusToControlNode(SaturationStatusMessage.WORKER_INFO_SATURATION_RUNNING);
                }
                toDo.add(axiom);
            }
        }

    }
}
