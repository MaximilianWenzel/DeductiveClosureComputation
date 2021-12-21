package reasoning.saturation.distributed.metadata;

import com.google.common.base.Stopwatch;
import enums.StatisticsComponent;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
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

    public static List<String> getControlNodeStatsHeader() {
        List<String> header = new ArrayList<>();
        header.add("NumberOfReceivedAxiomCountMessages");
        header.add("SumOfReceivedAxiomsEqualsSumOfSentAxiomsEvent");
        header.add("WorkerInitializationTimeMS");
        header.add("TotalSaturationTimeMS");
        header.add("CollectingClosureResultsTimeMS");
        return header;
    }

    public AtomicLong getNumberOfReceivedAxiomCountMessages() {
        return numberOfReceivedAxiomCountMessages;
    }

    public List<Long> getControlNodeStatistics() {
        List<Long> stats = new ArrayList<>();
        stats.add(numberOfReceivedAxiomCountMessages.get());
        stats.add(sumOfReceivedAxiomsEqualsSumOfSentAxiomsEvent.get());
        stats.add(workerInitializationTimeMS);
        stats.add(totalSaturationTimeMS);
        stats.add(collectingClosureResultsFromWorkersMS);
        return stats;
    }

}
