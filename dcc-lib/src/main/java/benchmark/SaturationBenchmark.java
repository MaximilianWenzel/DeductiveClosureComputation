package benchmark;


import com.esotericsoftware.kryonet.Server;
import com.google.common.base.Stopwatch;
import data.Closure;
import enums.SaturationApproach;
import networking.ServerData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.SingleThreadedSaturation;
import reasoning.saturation.distributed.DistributedSaturation;
import reasoning.saturation.distributed.SaturationWorker;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.parallel.ParallelSaturation;
import util.CSVUtils;
import util.ConsoleUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SaturationBenchmark<C extends Closure<A>, A extends Serializable, T extends Serializable> {
    private Logger log = ConsoleUtils.getLogger();

    private int numberOfExperimentRepetitions = 1;
    private Set<SaturationApproach> includedApproaches;
    private Stopwatch stopwatch;
    private File outputDirectory;
    private String benchmarkType;

    private List<List<String>> csvRows = new ArrayList<>();
    private List<String> csvHeader;


    private SaturationInitializationFactory<C, A, T> initializationFactory;
    private List<? extends A> initialAxioms;
    private List<WorkerModel<C, A, T>> workers;

    {
        csvHeader = new ArrayList<>();
        csvHeader.add("benchmarkType");
        csvHeader.add("approach");
        csvHeader.add("numberOfInitialAxioms");
        csvHeader.add("numWorkers");
        csvHeader.add("minRuntimeMS");
        csvHeader.add("maxRuntimeMS");
        csvHeader.add("averageRuntimeMS");
    }

    public SaturationBenchmark(String benchmarkType,
                               Set<SaturationApproach> includedApproaches,
                               File outputDirectory,
                               int numberOfExperimentRepetitions) {
        this.benchmarkType = benchmarkType;
        this.includedApproaches = includedApproaches;
        this.numberOfExperimentRepetitions = numberOfExperimentRepetitions;
        this.outputDirectory = outputDirectory;
        this.outputDirectory.mkdir();
    }


    public void startBenchmark(SaturationInitializationFactory<C, A, T> saturationInitializationFactory) {
        this.initializationFactory = saturationInitializationFactory;
        this.initialAxioms = initializationFactory.getInitialAxioms();
        this.workers = initializationFactory.getWorkerModels();

        log.info(ConsoleUtils.getSeparator());
        log.info("Starting experiment...");
        log.info(ConsoleUtils.getSeparator());

        // single threaded
        if (workers.size() == 1) {
            if (includedApproaches.contains(SaturationApproach.SINGLE_THREADED)) {
                runSingleThreadedSaturationBenchmark();
                initializationFactory.resetFactory();
            }
        }

        if (workers.size() > 1) {
            // parallel
            if (includedApproaches.contains(SaturationApproach.PARALLEL)) {
                runParallelSaturationBenchmark();
                initializationFactory.resetFactory();
            }

            // distributed - each worker in separate thread
            if (includedApproaches.contains(SaturationApproach.DISTRIBUTED_MULTITHREADED)) {
                runDistributedSaturationBenchmark(false);
                initializationFactory.resetFactory();
            }

            // distributed - each worker in separate JVM
            if (includedApproaches.contains(SaturationApproach.DISTRIBUTED_SEPARATE_JVM)) {
                runDistributedSaturationBenchmark(true);
                initializationFactory.resetFactory();
            }
        }

    }

    public void finishBenchmark() {
        try {
            createCSVFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runSingleThreadedSaturationBenchmark() {
        log.info("Single");
        log.info("# Initial Axioms: " + initialAxioms.size());

        DescriptiveStatistics runtimeInMSStats = singleThreadedClosureComputation();

        CSVRow row = new CSVRow(
                "single",
                initialAxioms.size(),
                null,
                runtimeInMSStats.getMin(),
                runtimeInMSStats.getMax(),
                runtimeInMSStats.getMean()
        );
        this.csvRows.add(row.toCSVRow());

        log.info(ConsoleUtils.getSeparator());

    }

    public void runParallelSaturationBenchmark() {
        log.info("Parallel");
        log.info("# Initial Axioms: " + initialAxioms.size());
        log.info("# Workers: " + workers.size());

        DescriptiveStatistics runtimeInMSStats = parallelClosureComputation(
        );

        CSVRow row = new CSVRow(
                "parallel",
                initialAxioms.size(),
                workers.size(),
                runtimeInMSStats.getMin(),
                runtimeInMSStats.getMax(),
                runtimeInMSStats.getMean()
        );

        this.csvRows.add(row.toCSVRow());

        log.info(ConsoleUtils.getSeparator());

    }

    public void runDistributedSaturationBenchmark(boolean workersInSeparateJVM) {
        DescriptiveStatistics runtime = null;
        log.info("Distributed");
        log.info("# Initial Axioms: " + initialAxioms.size());
        log.info("# Workers: " + workers.size());
        runtime = distributedClosureComputation(workersInSeparateJVM);

        CSVRow row = new CSVRow(
                "distributed",
                initialAxioms.size(),
                workers.size(),
                runtime.getMin(),
                runtime.getMax(),
                runtime.getMean()
        );

        csvRows.add(row.toCSVRow());

        log.info(ConsoleUtils.getSeparator());

    }

    private DescriptiveStatistics distributedClosureComputation(boolean workersInSeparateJVM) {
        List<Double> runtimeInMSPerRound = new ArrayList<>();

        for (int i = 1; i <= this.numberOfExperimentRepetitions; i++) {
            log.info("Round " + i);


            // initialize workers
            List<ServerData> serverDataList;
            if (workersInSeparateJVM) {
                serverDataList = generateWorkersInSeparateJVM();
            } else {
                serverDataList = generateWorkersInSeparateThread();
            }


            // initialize control node
            DistributedSaturation<C, A, T> saturation = new DistributedSaturation<>(
                    initializationFactory.getDistributedWorkerModels(serverDataList),
                    initializationFactory.getWorkloadDistributor(),
                    initialAxioms,
                    initializationFactory.getNewClosure(),
                    new SaturationConfiguration(true)
            );

            // run saturation
            C closure = saturation.saturate();
            assert closure.getClosureResults().size() > 0;

            ControlNodeStatistics controlNodeStatistics = saturation.getControlNodeStatistics();
            List<WorkerStatistics> workerStatistics = saturation.getWorkerStatistics();

            // runtime
            long runtimeInMS = controlNodeStatistics.getWorkerInitializationTimeMS()
                    + controlNodeStatistics.getTotalSaturationTimeMS();
            runtimeInMSPerRound.add((double) runtimeInMS);

            // distributed saturation stats
            String csvControlNodeStatsPath = Paths.get(this.outputDirectory.toString(), "distributed_controlNode"
                    + "_" + benchmarkType
                    + "_numMessages=" + initialAxioms.size()
                    + "_numWorkers=" + workers.size())
                    + ".csv";

            String csvWorkerStatsPath = Paths.get(this.outputDirectory.toString(), "distributed_workers"
                    + "_" + benchmarkType
                    + "_numMessages=" + initialAxioms.size()
                    + "_numWorkers=" + workers.size())
                    + ".csv";
            try {
                CSVUtils.writeCSVFile(csvControlNodeStatsPath, ControlNodeStatistics.getControlNodeStatsHeader(),
                        Collections.singletonList(controlNodeStatistics.getControlNodeStatistics()), ";");
                CSVUtils.writeCSVFile(csvWorkerStatsPath,
                        WorkerStatistics.getWorkerStatsHeader(),
                        workerStatistics.stream().map(WorkerStatistics::getWorkerStatistics)
                                .collect(Collectors.toList()),
                        ";");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new DescriptiveStatistics(runtimeInMSPerRound.stream().mapToDouble(d -> d).toArray());
    }

    private List<ServerData> generateWorkersInSeparateThread() {
        SaturationWorkerServerGenerator<C, A, T> workerServerFactory;

        workerServerFactory = new SaturationWorkerServerGenerator<>(workers.size());

        List<SaturationWorker<C, A, T>> saturationWorkers;
        saturationWorkers = workerServerFactory.generateWorkers();
        List<Thread> threads = saturationWorkers.stream().map(Thread::new).collect(Collectors.toList());
        threads.forEach(Thread::start);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return workerServerFactory.getServerDataList();
    }

    private List<ServerData> generateWorkersInSeparateJVM() {
        SaturationJVMWorkerGenerator<C, A, T> workerServerFactory;

        workerServerFactory = new SaturationJVMWorkerGenerator<>(workers.size());
        workerServerFactory.startWorkersInSeparateJVMs();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return workerServerFactory.getServerDataList();
    }

    private DescriptiveStatistics singleThreadedClosureComputation() {
        List<Double> runtimeInMSPerRound = new ArrayList<>();

        for (int i = 1; i <= this.numberOfExperimentRepetitions; i++) {
            log.info("Round " + i);

            SingleThreadedSaturation<C, A> saturation = new SingleThreadedSaturation<>(
                    initialAxioms.iterator(),
                    initializationFactory.generateRules(),
                    initializationFactory.getNewClosure()
            );

            // run saturation
            this.stopwatch = Stopwatch.createStarted();
            C closure = saturation.saturate();
            assert closure.getClosureResults().size() > 0;

            Duration runtime = this.stopwatch.elapsed();
            runtimeInMSPerRound.add((double) runtime.toMillis());

            assert closure.getClosureResults().size() > 0;

        }
        return new DescriptiveStatistics(runtimeInMSPerRound.stream().mapToDouble(d -> d).toArray());
    }

    private DescriptiveStatistics parallelClosureComputation() {
        List<Double> runtimeInMSPerRound = new ArrayList<>();

        for (int i = 1; i <= this.numberOfExperimentRepetitions; i++) {
            log.info("Round " + i);

            ParallelSaturation<C, A, T> saturation = new ParallelSaturation<>(
                    initializationFactory
            );

            // run saturation
            this.stopwatch = Stopwatch.createStarted();
            C closure = saturation.saturate();
            Duration runtime = this.stopwatch.elapsed();
            assert closure.getClosureResults().size() > 0;
            runtimeInMSPerRound.add((double) runtime.toMillis());
        }
        return new DescriptiveStatistics(runtimeInMSPerRound.stream().mapToDouble(d -> d).toArray());
    }

    private void createCSVFile() throws IOException {
        CSVFormat csvFormat = CSVFormat.Builder.create()
                .setHeader(this.csvHeader.toArray(new String[csvHeader.size()]))
                .setDelimiter(";")
                .build();
        FileWriter out = new FileWriter(benchmarkType + "_" + "benchmark.csv");
        try (CSVPrinter printer = new CSVPrinter(out, csvFormat)) {
            for (List<String> values : this.csvRows) {
                printer.printRecord(values);
            }
        }
    }

    private class CSVRow {
        String benchmarkType = SaturationBenchmark.this.benchmarkType;
        String approach;
        Integer numberOfEchoAxioms;
        Integer numWorkers;
        Double minRuntimeMS;
        Double maxRuntimeMS;
        Double averageRuntimeMS;

        public CSVRow(String approach, Integer numberOfEchoAxioms,
                      Integer numWorkers,
                      Double minRuntimeMS, Double maxRuntimeMS, Double averageRuntimeMS) {
            this.approach = approach;
            this.numberOfEchoAxioms = numberOfEchoAxioms;
            this.numWorkers = numWorkers;
            this.minRuntimeMS = minRuntimeMS;
            this.maxRuntimeMS = maxRuntimeMS;
            this.averageRuntimeMS = averageRuntimeMS;
        }

        protected List<String> toCSVRow() {
            List<String> row = new ArrayList<>();
            row.add(benchmarkType);
            row.add(approach);
            row.add(numberOfEchoAxioms + "");
            row.add(numWorkers + "");
            row.add(minRuntimeMS + "");
            row.add(maxRuntimeMS + "");
            row.add(averageRuntimeMS + "");

            assert row.size() == csvHeader.size();
            return row;
        }
    }

}

