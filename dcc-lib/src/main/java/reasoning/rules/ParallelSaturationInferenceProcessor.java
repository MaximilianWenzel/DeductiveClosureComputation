package reasoning.rules;

import data.Closure;
import enums.StatisticsComponent;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.parallel.SaturationContext;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ParallelSaturationInferenceProcessor<C extends Closure<A>, A extends Serializable>
        implements InferenceProcessor<A> {

    private final WorkloadDistributor<C, A> distributor;
    private Map<Long, SaturationContext<C, A>> workerIDToSaturationContext;
    private C closure;
    private AtomicInteger sentAxiomsCount;
    private WorkerStatistics statistics = null;

    public ParallelSaturationInferenceProcessor(WorkloadDistributor<C, A> distributor,
                                                Map<Long, SaturationContext<C, A>> workerIDToSaturationContext,
                                                C closure,
                                                AtomicInteger sentAxiomsCount) {
        this.distributor = distributor;
        this.workerIDToSaturationContext = workerIDToSaturationContext;
        this.closure = closure;
        this.sentAxiomsCount = sentAxiomsCount;
    }

    public ParallelSaturationInferenceProcessor(WorkerStatistics statistics,
                                                WorkloadDistributor<C, A> distributor,
                                                Map<Long, SaturationContext<C, A>> workerIDToSaturationContext,
                                                C closure,
                                                AtomicInteger sentAxiomsCount) {
        this.statistics = statistics;
        this.distributor = distributor;
        this.workerIDToSaturationContext = workerIDToSaturationContext;
        this.closure = closure;
        this.sentAxiomsCount = sentAxiomsCount;
    }

    @Override
    public void processInference(A axiom) {
        if (statistics != null) {
            statistics.startStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
            statistics.getNumberOfDerivedInferences().incrementAndGet();
        }

        // distribute axiom only if it is not contained in closure
        Stream<Long> workerIDs = distributor.getRelevantWorkerIDsForAxiom(axiom);
        workerIDs.forEach(workerID -> {
            sentAxiomsCount.incrementAndGet();
            SaturationContext<C, A> saturationContext = workerIDToSaturationContext.get(workerID);
            BlockingQueue<Object> toDo = saturationContext.getToDo();
            toDo.add(axiom);
        });

        if (statistics != null) {
            statistics.stopStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
        }
    }
}
