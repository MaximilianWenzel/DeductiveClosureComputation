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

/**
 * This class is used in the multi-threaded closure computation procedure in order to distribute new generated conclusions to appropriate
 * workers.
 *
 * @param <C> Type of the resulting deductive closure.
 * @param <A> Type of the axioms in the deductive closure.
 */
public class MultithreadedSaturationConclusionProcessor<C extends Closure<A>, A extends Serializable>
        implements ConclusionProcessor<A> {

    private final WorkloadDistributor<C, A> distributor;
    private final Map<Long, SaturationContext<C, A>> workerIDToSaturationContext;
    private final C closure;
    private final AtomicInteger sentAxiomsCount;
    private WorkerStatistics statistics = null;

    public MultithreadedSaturationConclusionProcessor(WorkloadDistributor<C, A> distributor,
                                                      Map<Long, SaturationContext<C, A>> workerIDToSaturationContext,
                                                      C closure,
                                                      AtomicInteger sentAxiomsCount) {
        this.distributor = distributor;
        this.workerIDToSaturationContext = workerIDToSaturationContext;
        this.closure = closure;
        this.sentAxiomsCount = sentAxiomsCount;
    }

    public MultithreadedSaturationConclusionProcessor(WorkerStatistics statistics,
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
    public void processConclusion(A axiom) {
        if (statistics != null) {
            statistics.startStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
            statistics.getNumberOfDerivedConclusions().incrementAndGet();
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
