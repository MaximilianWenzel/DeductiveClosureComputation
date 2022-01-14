package reasoning.rules;

import data.Closure;
import data.ToDoQueue;
import enums.StatisticsComponent;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.parallel.SaturationContext;
import reasoning.saturation.workload.WorkloadDistributor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ParallelSaturationInferenceProcessor<C extends Closure<A>, A extends Serializable, T extends Serializable>
        implements InferenceProcessor<A> {

    private final WorkloadDistributor<C, A, T> distributor;
    private Map<Long, SaturationContext<C, A, T>> workerIDToSaturationContext;
    private C closure;
    private AtomicInteger sentAxiomsCount;
    private WorkerStatistics statistics = null;

    public ParallelSaturationInferenceProcessor(WorkloadDistributor<C, A, T> distributor,
                                                Map<Long, SaturationContext<C, A, T>> workerIDToSaturationContext,
                                                C closure,
                                                AtomicInteger sentAxiomsCount) {
        this.distributor = distributor;
        this.workerIDToSaturationContext = workerIDToSaturationContext;
        this.closure = closure;
        this.sentAxiomsCount = sentAxiomsCount;
    }

    public ParallelSaturationInferenceProcessor(WorkerStatistics statistics,
                                                WorkloadDistributor<C, A, T> distributor,
                                                Map<Long, SaturationContext<C, A, T>> workerIDToSaturationContext,
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
        // distribute axiom only if it is not contained in closure
        if (statistics != null) {
            statistics.startStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
            statistics.getNumberOfDerivedInferences().incrementAndGet();
        }

        if (!closure.contains(axiom)) {
            Stream<Long> workerIDs = distributor.getRelevantWorkerIDsForAxiom(axiom);
            workerIDs.forEach(workerID -> {
                sentAxiomsCount.incrementAndGet();
                if (statistics != null) {
                    statistics.getNumberOfSentAxioms().incrementAndGet();
                }
                SaturationContext<C, A, T> saturationContext = workerIDToSaturationContext.get(workerID);
                ToDoQueue<Serializable> toDo = saturationContext.getToDo();
                toDo.add(axiom);
            });
        }

        if (statistics != null) {
            statistics.stopStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
        }
    }
}
