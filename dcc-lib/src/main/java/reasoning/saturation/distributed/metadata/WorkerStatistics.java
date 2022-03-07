package reasoning.saturation.distributed.metadata;

import com.google.common.base.Stopwatch;
import enums.MessageDistributionType;
import enums.StatisticsComponent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class can be used in order to collect finegrained statistics for the workers in the computation of the deductive
 * closure for a given set of rules and axioms.
 */
public class WorkerStatistics implements Serializable {
    private final AtomicLong todoIsEmptyEvent = new AtomicLong(0);
    private final AtomicLong numberOfProcessedAxioms = new AtomicLong(0);
    private final AtomicLong numberOfDerivedConclusions = new AtomicLong(0);
    private final AtomicLong numberOfSentAxioms = new AtomicLong(0);
    private final AtomicLong numberOfReceivedAxioms = new AtomicLong(0);
    // worker statistics
    transient Stopwatch initializingConnectionsToOtherWorkersSW = Stopwatch.createUnstarted();
    transient Stopwatch waitingTimeWhileSaturationSW = Stopwatch.createUnstarted();
    transient Stopwatch applyingRulesTimeWhileSaturationSW = Stopwatch.createUnstarted();
    transient Stopwatch distributingAxiomsTime = Stopwatch.createUnstarted();
    private long initializingConnectionsToOtherWorkersMS = -1L;
    private long waitingTimeWhileSaturationMS = -1L;
    private long applyingRulesTimeWhileSaturationMS = -1L;
    private long distributingAxiomsTimeMS = -1L;

    public WorkerStatistics() {

    }

    public static List<String> getWorkerStatsHeader() {
        List<String> statsHeader = new ArrayList<>();
        statsHeader.add("WorkerInitializingConnectionsToOtherWorkersMS");
        statsHeader.add("WorkerWaitingTimeWhileSaturationMS");
        statsHeader.add("ApplyingRulesTimeWhileSaturationMS");
        statsHeader.add("DistributingAxiomsTimeMS");
        statsHeader.add("ToDoIsEmptyEvent");
        statsHeader.add("NumberOfProcessedAxioms");
        statsHeader.add("NumberOfDerivedInferences");
        statsHeader.add("NumberOfSentAxioms");
        statsHeader.add("NumberOfReceivedAxioms");
        return statsHeader;
    }

    public static List<String> getWorkerStatsSummaryHeader() {
        List<String> statsHeader = new ArrayList<>();
        statsHeader.add("benchmarkType");
        statsHeader.add("approach");
        statsHeader.add("numWorkers");
        statsHeader.add("messageDistribution");
        statsHeader.add("AVGWorkerInitializingConnectionsToOtherWorkersMS");
        statsHeader.add("AVGWorkerWaitingTimeWhileSaturationMS");
        statsHeader.add("AVGApplyingRulesTimeWhileSaturationMS");
        statsHeader.add("AVGDistributingAxiomsTimeMS");
        statsHeader.add("AVGToDoIsEmptyEvent");
        statsHeader.add("AVGNumberOfProcessedAxioms");
        statsHeader.add("AVGNumberOfDerivedInferences");
        statsHeader.add("AVGNumberOfSentAxioms");
        statsHeader.add("AVGNumberOfReceivedAxioms");
        return statsHeader;
    }

    public static List<String> getSummarizedWorkerStatistics(List<WorkerStatistics> stats, String benchmarkType, String approach,
                                                             long numberOfWorkers, MessageDistributionType messageDistributionType) {
        List<String> result = new ArrayList<>();
        result.add(benchmarkType);
        result.add(approach);
        result.add("" + numberOfWorkers);
        result.add(messageDistributionType.toString().toLowerCase());
        result.add("" + stats.stream().mapToDouble(WorkerStatistics::getInitializingConnectionsToOtherWorkersMS).average().getAsDouble());
        result.add("" + stats.stream().mapToDouble(WorkerStatistics::getWaitingTimeWhileSaturationMS).average().getAsDouble());
        result.add("" + stats.stream().mapToDouble(WorkerStatistics::getApplyingRulesTimeWhileSaturationMS).average().getAsDouble());
        result.add("" + stats.stream().mapToDouble(WorkerStatistics::getDistributingAxiomsTimeMS).average().getAsDouble());
        result.add("" + stats.stream().mapToDouble(s -> s.getTodoIsEmptyEvent().get()).average().getAsDouble());
        result.add("" + stats.stream().mapToDouble(s -> s.getNumberOfProcessedAxioms().get()).average().getAsDouble());
        result.add("" + stats.stream().mapToDouble(s -> s.getNumberOfDerivedConclusions().get()).average().getAsDouble());
        result.add("" + stats.stream().mapToDouble(s -> s.getNumberOfSentAxioms().get()).average().getAsDouble());
        result.add("" + stats.stream().mapToDouble(s -> s.getNumberOfReceivedAxioms().get()).average().getAsDouble());
        return result;
    }

    public void startStopwatch(StatisticsComponent component) {
        switch (component) {
            case WORKER_INITIALIZING_OTHER_WORKER_CONNECTIONS:
                initializingConnectionsToOtherWorkersSW.start();
                break;
            case WORKER_WAITING_TIME_SATURATION:
                waitingTimeWhileSaturationSW.start();
                break;
            case WORKER_APPLYING_RULES_TIME_SATURATION:
                applyingRulesTimeWhileSaturationSW.start();
                break;
            case WORKER_DISTRIBUTING_AXIOMS_TIME:
                distributingAxiomsTime.start();
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void stopStopwatch(StatisticsComponent component) {
        switch (component) {
            case WORKER_INITIALIZING_OTHER_WORKER_CONNECTIONS:
                initializingConnectionsToOtherWorkersSW.stop();
                break;
            case WORKER_WAITING_TIME_SATURATION:
                waitingTimeWhileSaturationSW.stop();
                break;
            case WORKER_APPLYING_RULES_TIME_SATURATION:
                applyingRulesTimeWhileSaturationSW.stop();
                break;
            case WORKER_DISTRIBUTING_AXIOMS_TIME:
                distributingAxiomsTime.stop();
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public AtomicLong getTodoIsEmptyEvent() {
        return todoIsEmptyEvent;
    }

    public AtomicLong getNumberOfProcessedAxioms() {
        return numberOfProcessedAxioms;
    }

    public AtomicLong getNumberOfDerivedConclusions() {
        return numberOfDerivedConclusions;
    }

    public AtomicLong getNumberOfSentAxioms() {
        return numberOfSentAxioms;
    }

    public AtomicLong getNumberOfReceivedAxioms() {
        return numberOfReceivedAxioms;
    }

    public void collectStopwatchTimes() {
        initializingConnectionsToOtherWorkersMS = initializingConnectionsToOtherWorkersSW.elapsed(
                TimeUnit.MILLISECONDS);
        waitingTimeWhileSaturationMS = waitingTimeWhileSaturationSW.elapsed(TimeUnit.MILLISECONDS);
        applyingRulesTimeWhileSaturationMS = applyingRulesTimeWhileSaturationSW.elapsed(
                TimeUnit.MILLISECONDS);
        distributingAxiomsTimeMS = distributingAxiomsTime.elapsed(TimeUnit.MILLISECONDS);
    }

    public List<Long> getWorkerStatistics() {
        List<Long> stats = new ArrayList<>();
        stats.add(initializingConnectionsToOtherWorkersMS);
        stats.add(waitingTimeWhileSaturationMS);
        stats.add(applyingRulesTimeWhileSaturationMS);
        stats.add(distributingAxiomsTimeMS);
        stats.add(todoIsEmptyEvent.get());
        stats.add(numberOfProcessedAxioms.get());
        stats.add(numberOfDerivedConclusions.get());
        stats.add(numberOfSentAxioms.get());
        stats.add(numberOfReceivedAxioms.get());
        return stats;
    }

    private long getInitializingConnectionsToOtherWorkersMS() {
        return initializingConnectionsToOtherWorkersMS;
    }

    private long getWaitingTimeWhileSaturationMS() {
        return waitingTimeWhileSaturationMS;
    }

    private long getApplyingRulesTimeWhileSaturationMS() {
        return applyingRulesTimeWhileSaturationMS;
    }

    private long getDistributingAxiomsTimeMS() {
        return distributingAxiomsTimeMS;
    }
}
