package reasoning.saturation.distributed.metadata;

import com.google.common.base.Stopwatch;
import enums.MessageDistributionType;
import enums.StatisticsComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ControlNodeStatistics {

    transient Stopwatch workerInitializationTimeSW = Stopwatch.createUnstarted();
    transient Stopwatch totalSaturationTimeSW = Stopwatch.createUnstarted();
    transient Stopwatch collectingClosureResultsFromWorkersSW = Stopwatch.createUnstarted();
    private long workerInitializationTimeMS = -1L;
    private long totalSaturationTimeMS = -1L;
    private long collectingClosureResultsFromWorkersMS = -1L;

    private AtomicLong numberOfReceivedAxiomCountMessages = new AtomicLong(0);
    private AtomicLong sumOfReceivedAxiomsEqualsSumOfSentAxiomsEvent = new AtomicLong(0);

    public static List<String> getControlNodeStatsHeader() {
        List<String> header = new ArrayList<>();
        header.add("benchmarkType");
        header.add("approach");
        header.add("numWorkers");
        header.add("messageDistribution");
        header.add("NumberOfReceivedAxiomCountMessages");
        header.add("SumOfReceivedAxiomsEqualsSumOfSentAxiomsEvent");
        header.add("WorkerInitializationTimeMS");
        header.add("TotalSaturationTimeMS");
        header.add("CollectingClosureResultsTimeMS");
        return header;
    }

    public void startStopwatch(StatisticsComponent component) {
        switch (component) {
            case CONTROL_NODE_INITIALIZING_ALL_WORKERS:
                workerInitializationTimeSW.start();
                break;
            case CONTROL_NODE_SATURATION_TIME:
                totalSaturationTimeSW.start();
                break;
            case CONTROL_NODE_WAITING_FOR_CLOSURE_RESULTS:
                collectingClosureResultsFromWorkersSW.start();
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void stopStopwatch(StatisticsComponent component) {
        switch (component) {
            case CONTROL_NODE_INITIALIZING_ALL_WORKERS:
                workerInitializationTimeSW.stop();
                break;
            case CONTROL_NODE_SATURATION_TIME:
                totalSaturationTimeSW.stop();
                break;
            case CONTROL_NODE_WAITING_FOR_CLOSURE_RESULTS:
                collectingClosureResultsFromWorkersSW.stop();
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    public void collectStopwatchTimes() {
        workerInitializationTimeMS = workerInitializationTimeSW.elapsed(TimeUnit.MILLISECONDS);
        collectingClosureResultsFromWorkersMS = collectingClosureResultsFromWorkersSW.elapsed(
                TimeUnit.MILLISECONDS);
        totalSaturationTimeMS = totalSaturationTimeSW.elapsed(TimeUnit.MILLISECONDS);
    }

    public AtomicLong getSumOfReceivedAxiomsEqualsSumOfSentAxiomsEvent() {
        return sumOfReceivedAxiomsEqualsSumOfSentAxiomsEvent;
    }

    public long getWorkerInitializationTimeMS() {
        return workerInitializationTimeMS;
    }

    public long getTotalSaturationTimeMS() {
        return totalSaturationTimeMS;
    }

    public long getCollectingClosureResultsFromWorkersMS() {
        return collectingClosureResultsFromWorkersMS;
    }

    public AtomicLong getNumberOfReceivedAxiomCountMessages() {
        return numberOfReceivedAxiomCountMessages;
    }

    public List<String> getControlNodeStatistics(String benchmarkType, String approach,
                                               long numberOfWorkers, MessageDistributionType messageDistributionType) {
        List<String> stats = new ArrayList<>();
        stats.add(benchmarkType);
        stats.add(approach);
        stats.add(messageDistributionType.toString().toLowerCase());
        stats.add("" + numberOfWorkers);
        stats.add("" + numberOfReceivedAxiomCountMessages.get());
        stats.add("" + sumOfReceivedAxiomsEqualsSumOfSentAxiomsEvent.get());
        stats.add("" + workerInitializationTimeMS);
        stats.add("" + totalSaturationTimeMS);
        stats.add("" + collectingClosureResultsFromWorkersMS);
        return stats;
    }

}
