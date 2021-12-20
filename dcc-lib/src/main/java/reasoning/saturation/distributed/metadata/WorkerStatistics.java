package reasoning.saturation.distributed.metadata;

import com.google.common.base.Stopwatch;
import enums.StatisticsComponent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class WorkerStatistics implements Serializable {
    // worker statistics
    transient Stopwatch initializingConnectionsToOtherWorkersSW = Stopwatch.createUnstarted();
    transient Stopwatch waitingTimeWhileSaturationSW = Stopwatch.createUnstarted();
    transient Stopwatch applyingRulesTimeWhileSaturationSW = Stopwatch.createUnstarted();
    private long initializingConnectionsToOtherWorkersMS = -1L;
    private long waitingTimeWhileSaturationMS = -1L;
    private long applyingRulesTimeWhileSaturationMS = -1L;
    private AtomicLong todoIsEmptyEvent = new AtomicLong(0);
    private AtomicLong numberOfProcessedAxioms = new AtomicLong(0);
    private AtomicLong numberOfDerivedInferences = new AtomicLong(0);
    private AtomicLong numberOfSentAxioms = new AtomicLong(0);
    private AtomicLong numberOfReceivedAxioms = new AtomicLong(0);

    public WorkerStatistics() {

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

    public AtomicLong getNumberOfDerivedInferences() {
        return numberOfDerivedInferences;
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
    }


    public static List<String> getWorkerStatsHeader() {
        List<String> statsHeader = new ArrayList<>();
        statsHeader.add("WorkerInitializingConnectionsToOtherWorkersMS");
        statsHeader.add("WorkerWaitingTimeWhileSaturationMS");
        statsHeader.add("ApplyingRulesTimeWhileSaturationMS");
        statsHeader.add("ToDoIsEmptyEvent");
        statsHeader.add("NumberOfProcessedAxioms");
        statsHeader.add("NumberOfDerivedInferences");
        statsHeader.add("NumberOfSentAxioms");
        statsHeader.add("NumberOfReceivedAxioms");
        return statsHeader;
    }

    public List<Long> getWorkerStatistics() {
        List<Long> stats = new ArrayList<>();
        stats.add(initializingConnectionsToOtherWorkersMS);
        stats.add(waitingTimeWhileSaturationMS);
        stats.add(applyingRulesTimeWhileSaturationMS);
        stats.add(todoIsEmptyEvent.get());
        stats.add(numberOfProcessedAxioms.get());
        stats.add(numberOfDerivedInferences.get());
        stats.add(numberOfSentAxioms.get());
        stats.add(numberOfReceivedAxioms.get());
        return stats;
    }
}
