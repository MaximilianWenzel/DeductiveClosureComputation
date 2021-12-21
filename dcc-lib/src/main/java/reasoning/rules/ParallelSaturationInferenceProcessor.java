package reasoning.rules;

import data.Closure;
import data.ToDoQueue;
import reasoning.saturation.parallel.SaturationContext;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelSaturationInferenceProcessor<C extends Closure<A>, A extends Serializable, T extends Serializable>
        implements InferenceProcessor<A> {

    private final WorkloadDistributor<C, A, T> distributor;
    private Map<Long, SaturationContext<C, A, T>> workerIDToSaturationContext;
    private C closure;
    private AtomicInteger sentAxiomsCount;

    public ParallelSaturationInferenceProcessor(WorkloadDistributor<C, A, T> distributor,
                                                Map<Long, SaturationContext<C, A, T>> workerIDToSaturationContext,
                                                C closure,
                                                AtomicInteger sentAxiomsCount) {
        this.distributor = distributor;
        this.workerIDToSaturationContext = workerIDToSaturationContext;
        this.closure = closure;
        this.sentAxiomsCount = sentAxiomsCount;
    }

    @Override
    public void processInference(A axiom) {
        // distribute axiom only if it is not contained in closure

        if (closure.contains(axiom)) {
            return;
        }
        List<Long> workerIDs = distributor.getRelevantWorkerIDsForAxiom(axiom);
        for (Long workerID : workerIDs) {
            sentAxiomsCount.incrementAndGet();
            SaturationContext<C, A, T> saturationContext = workerIDToSaturationContext.get(workerID);
            ToDoQueue<Serializable> toDo = saturationContext.getToDo();
            toDo.add(axiom);
        }
    }
}
