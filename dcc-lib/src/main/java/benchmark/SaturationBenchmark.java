package benchmark;


import benchmark.workergeneration.SaturationDockerWorkerGenerator;
import benchmark.workergeneration.SaturationJVMWorkerGenerator;
import benchmark.workergeneration.SaturationWorkerGenerator;
import benchmark.workergeneration.SaturationWorkerThreadGenerator;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Streams;
import data.Closure;
import enums.MessageDistributionType;
import enums.SaturationApproach;
import networking.ServerData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.SingleThreadedSaturation;
import reasoning.saturation.distributed.DistributedSaturation;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.DistributedSaturationConfiguration;
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
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SaturationBenchmark<C extends Closure<A>, A extends Serializable, T extends Serializable> {
    private static final List<List<String>> csvRows = new ArrayList<>();
    private Logger log = ConsoleUtils.getLogger();
    private int numberOfExperimentRepetitions = 2;
    private int numberOfWarmUpRounds = 2;
    private Set<SaturationApproach> includedApproaches;
    private Stopwatch stopwatch;
    private File outputDirectory;
    private String benchmarkType;
    private List<MessageDistributionType> messageDistributionTypes;
    private boolean workerNodeStatistics = false;
    private SaturationInitializationFactory<C, A, T> initializationFactory;
    private Iterator<? extends A> initialAxioms;
    private long numberOfInitialAxioms;
    private List<WorkerModel<C, A, T>> workers;
    private SaturationWorkerGenerator workerGenerator;
    private List<String> csvHeader;

    {
        this.csvHeader = new ArrayList<>();
        this.csvHeader.add("benchmarkType");
        this.csvHeader.add("approach");
        this.csvHeader.add("messageDistribution");
        this.csvHeader.add("withSendingClosureResultsTime");
        this.csvHeader.add("numberOfInitialAxioms");
        this.csvHeader.add("numWorkers");
        this.csvHeader.add("minRuntimeMS");
        this.csvHeader.add("maxRuntimeMS");
        this.csvHeader.add("averageRuntimeMS");
    }


    public SaturationBenchmark(String benchmarkType,
                               Set<SaturationApproach> includedApproaches,
                               File outputDirectory,
                               int numberOfWarmUpRounds, int numberOfExperimentRepetitions,
                               boolean workerNodeStatistics,
                               List<MessageDistributionType> messageDistributionTypes) {
        this.benchmarkType = benchmarkType;
        this.messageDistributionTypes = messageDistributionTypes;
        this.numberOfWarmUpRounds = numberOfWarmUpRounds;
        this.includedApproaches = includedApproaches;
        this.numberOfExperimentRepetitions = numberOfExperimentRepetitions;
        this.outputDirectory = outputDirectory;
        this.outputDirectory.mkdir();
        this.workerNodeStatistics = workerNodeStatistics;
    }

    public SaturationBenchmark(String benchmarkType,
                               Set<SaturationApproach> includedApproaches,
                               File outputDirectory,
                               int numberOfExperimentRepetitions,
                               boolean workerNodeStatistics) {
        this.benchmarkType = benchmarkType;
        this.includedApproaches = includedApproaches;
        this.numberOfExperimentRepetitions = numberOfExperimentRepetitions;
        this.outputDirectory = outputDirectory;
        this.outputDirectory.mkdir();
        this.workerNodeStatistics = workerNodeStatistics;
    }

    public void startBenchmark(SaturationInitializationFactory<C, A, T> saturationInitializationFactory) {
        this.initializationFactory = saturationInitializationFactory;

        this.initialAxioms = initializationFactory.getInitialAxioms();
        this.numberOfInitialAxioms = Streams.stream(initialAxioms).count();
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

            for (MessageDistributionType messageDistributionType : messageDistributionTypes) {
                // distributed - each worker in separate thread
                if (includedApproaches.contains(SaturationApproach.DISTRIBUTED_MULTITHREADED)) {
                    runDistributedSaturationBenchmark(SaturationApproach.DISTRIBUTED_MULTITHREADED, messageDistributionType);
                    initializationFactory.resetFactory();
                }

                // distributed - each worker in separate JVM
                if (includedApproaches.contains(SaturationApproach.DISTRIBUTED_SEPARATE_JVM)) {
                    runDistributedSaturationBenchmark(SaturationApproach.DISTRIBUTED_SEPARATE_JVM, messageDistributionType);
                    initializationFactory.resetFactory();
                }

                // distributed - each worker in separate docker container
                if (includedApproaches.contains(SaturationApproach.DISTRIBUTED_SEPARATE_DOCKER_CONTAINER)) {
                    runDistributedSaturationBenchmark(SaturationApproach.DISTRIBUTED_SEPARATE_DOCKER_CONTAINER, messageDistributionType);
                    initializationFactory.resetFactory();
                }

                // distributed - worker have been already started in separate docker container, only control node is started
                if (includedApproaches.contains(SaturationApproach.DISTRIBUTED_DOCKER_BENCHMARK)) {
                    runDistributedSaturationBenchmark(SaturationApproach.DISTRIBUTED_DOCKER_BENCHMARK, messageDistributionType);
                    initializationFactory.resetFactory();
                }
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
        log.info("# Initial Axioms: " + numberOfInitialAxioms);

        DescriptiveStatistics runtimeInMSStats = singleThreadedClosureComputation();

        CSVRow row = new CSVRow(
                "single",
                numberOfInitialAxioms,
                MessageDistributionType.ADD_OWN_MESSAGES_DIRECTLY_TO_TODO,
                true,
                1,
                runtimeInMSStats.getMin(),
                runtimeInMSStats.getMax(),
                runtimeInMSStats.getMean()
        );
        this.csvRows.add(row.toCSVRow());

        log.info(ConsoleUtils.getSeparator());

    }

    public void runParallelSaturationBenchmark() {
        log.info("Parallel");
        log.info("# Initial Axioms: " + numberOfInitialAxioms);
        log.info("# Workers: " + workers.size());

        RuntimeMeasurements runtime = parallelClosureComputation();

        DescriptiveStatistics measurements = runtime.getRuntimeWithSendingClosureResults();
        CSVRow rowWithSendingClosureResults = new CSVRow(
                "parallel",
                numberOfInitialAxioms,
                MessageDistributionType.ADD_OWN_MESSAGES_DIRECTLY_TO_TODO,
                true,
                workers.size(),
                measurements.getMin(),
                measurements.getMax(),
                measurements.getMean()
        );

        measurements = runtime.getRuntimeWithoutSendingClosureResults();
        CSVRow rowWithoutSendingClosureResults = new CSVRow(
                "parallel",
                numberOfInitialAxioms,
                MessageDistributionType.ADD_OWN_MESSAGES_DIRECTLY_TO_TODO,
                false,
                workers.size(),
                measurements.getMin(),
                measurements.getMax(),
                measurements.getMean()
        );

        csvRows.add(rowWithSendingClosureResults.toCSVRow());
        csvRows.add(rowWithoutSendingClosureResults.toCSVRow());

        log.info(ConsoleUtils.getSeparator());

    }

    public void runDistributedSaturationBenchmark(SaturationApproach distributedApproach, MessageDistributionType messageDistributionType) {
        RuntimeMeasurements runtime = null;
        log.info("Distributed");
        log.info("# Initial Axioms: " + numberOfInitialAxioms);
        log.info("# Workers: " + workers.size());
        log.info("Approach: " + distributedApproach.toString());
        try {
            runtime = distributedClosureComputation(distributedApproach, messageDistributionType);
            DescriptiveStatistics measurements = runtime.getRuntimeWithSendingClosureResults();
            CSVRow rowWithSendingClosureResults = new CSVRow(
                    distributedApproach.toString().toLowerCase(),
                    numberOfInitialAxioms,
                    messageDistributionType,
                    true,
                    workers.size(),
                    measurements.getMin(),
                    measurements.getMax(),
                    measurements.getMean()
            );

            measurements = runtime.getRuntimeWithoutSendingClosureResults();
            CSVRow rowWithoutSendingClosureResults = new CSVRow(
                    distributedApproach.toString().toLowerCase(),
                    numberOfInitialAxioms,
                    messageDistributionType,
                    false,
                    workers.size(),
                    measurements.getMin(),
                    measurements.getMax(),
                    measurements.getMean()
            );

            csvRows.add(rowWithSendingClosureResults.toCSVRow());
            csvRows.add(rowWithoutSendingClosureResults.toCSVRow());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info(ConsoleUtils.getSeparator());

    }

    private RuntimeMeasurements distributedClosureComputation(SaturationApproach distributedApproach,
                                                              MessageDistributionType messageDistributionType) throws
            InterruptedException {
        List<Double> runtimeInMSPerRound = new ArrayList<>();
        List<Double> runtimeInMSPerRoundWithSendingClosure = new ArrayList<>();
        List<ServerData> serverDataList = null;

        // initialize workers
        switch (distributedApproach) {
            case DISTRIBUTED_MULTITHREADED:
                workerGenerator = new SaturationWorkerThreadGenerator(workers.size(), 1);
                break;
            case DISTRIBUTED_SEPARATE_DOCKER_CONTAINER:
                workerGenerator = new SaturationDockerWorkerGenerator(workers.size());
                break;
            case DISTRIBUTED_SEPARATE_JVM:
                workerGenerator = new SaturationJVMWorkerGenerator(workers.size(), 1);
                break;
            case DISTRIBUTED_DOCKER_BENCHMARK:
                workerGenerator = null;
                serverDataList = new ArrayList<>();
                for (int i = 0; i < workers.size(); i++) {
                    String serverName = "dcc-lib_worker_" + (i + 2);
                    serverDataList.add(new ServerData(serverName, 30_000));
                }
                break;
            default:
                throw new IllegalArgumentException();
        }

        for (int roundNumber = 1; roundNumber <= this.numberOfWarmUpRounds + this.numberOfExperimentRepetitions; roundNumber++) {
            initialAxioms = this.initializationFactory.getInitialAxioms();

            if (roundNumber <= numberOfWarmUpRounds) {
                log.info("Warm-up Round " + roundNumber);
            } else {
                log.info("Round " + (roundNumber - numberOfWarmUpRounds));
            }

            if (serverDataList == null) {
                workerGenerator.generateAndRunWorkers();
                serverDataList = workerGenerator.getWorkerServerDataList();
            }

            // initialize control node
            DistributedSaturation<C, A, T> saturation = new DistributedSaturation<>(
                    initializationFactory.getDistributedWorkerModels(serverDataList),
                    initializationFactory.getWorkloadDistributor(),
                    initialAxioms,
                    initializationFactory.getNewClosure(),
                    new DistributedSaturationConfiguration(true, workerNodeStatistics, messageDistributionType),
                    1
            );

            // run saturation
            C closure = saturation.saturate();
            assert closure.getClosureResults().size() > 0;

            ControlNodeStatistics controlNodeStatistics = saturation.getControlNodeStatistics();
            List<WorkerStatistics> workerStatistics = saturation.getWorkerStatistics();

            // runtime
            if (roundNumber > numberOfWarmUpRounds) {
                long runtimeInMS = controlNodeStatistics.getWorkerInitializationTimeMS()
                        + controlNodeStatistics.getTotalSaturationTimeMS();
                runtimeInMSPerRound.add((double) runtimeInMS);
                runtimeInMSPerRoundWithSendingClosure.add(
                        (double) runtimeInMS + controlNodeStatistics.getCollectingClosureResultsFromWorkersMS());
            }

            // distributed saturation stats
            createStatisticsCSVFiles(distributedApproach.toString().toLowerCase(), controlNodeStatistics,
                    workerStatistics);
        }

        // stop workers
        if (!distributedApproach.equals(SaturationApproach.DISTRIBUTED_DOCKER_BENCHMARK)) {
            workerGenerator.stopWorkers();
        }
        RuntimeMeasurements runtimeMeasurements = new RuntimeMeasurements(
                new DescriptiveStatistics(runtimeInMSPerRoundWithSendingClosure.stream().mapToDouble(d -> d).toArray()),
                new DescriptiveStatistics(runtimeInMSPerRound.stream().mapToDouble(d -> d).toArray())
        );
        return runtimeMeasurements;
    }

    private DescriptiveStatistics singleThreadedClosureComputation() {
        List<Double> runtimeInMSPerRound = new ArrayList<>();

        for (int roundNumber = 1; roundNumber <= this.numberOfWarmUpRounds + this.numberOfExperimentRepetitions; roundNumber++) {
            initialAxioms = this.initializationFactory.getInitialAxioms();

            if (roundNumber <= numberOfWarmUpRounds) {
                log.info("Warm-up Round " + roundNumber);
            } else {
                log.info("Round " + (roundNumber - numberOfWarmUpRounds));
            }


            SingleThreadedSaturation<C, A> saturation = new SingleThreadedSaturation<>(
                    initialAxioms,
                    initializationFactory.generateRules(),
                    initializationFactory.getNewClosure()
            );

            // run saturation
            this.stopwatch = Stopwatch.createStarted();
            C closure = saturation.saturate();
            assert closure.getClosureResults().size() > 0;
            this.stopwatch.stop();

            if (roundNumber > numberOfWarmUpRounds) {
                Duration runtime = this.stopwatch.elapsed();
                runtimeInMSPerRound.add((double) runtime.toMillis());
            }
        }
        return new DescriptiveStatistics(runtimeInMSPerRound.stream().mapToDouble(d -> d).toArray());
    }

    private RuntimeMeasurements parallelClosureComputation() {
        List<Double> runtimeInMSPerRound = new ArrayList<>();
        List<Double> runtimeInMSPerRoundWithSendingClosure = new ArrayList<>();

        for (int roundNumber = 1; roundNumber <= this.numberOfWarmUpRounds + this.numberOfExperimentRepetitions; roundNumber++) {
            if (roundNumber <= numberOfWarmUpRounds) {
                log.info("Warm-up Round " + roundNumber);
            } else {
                log.info("Round " + (roundNumber - numberOfWarmUpRounds));
            }

            ParallelSaturation<C, A, T> saturation = new ParallelSaturation<>(
                    new SaturationConfiguration(true, workerNodeStatistics),
                    initializationFactory
            );

            // run saturation
            C closure = saturation.saturate();
            assert closure.getClosureResults().size() > 0;
            ControlNodeStatistics controlNodeStatistics = saturation.getControlNodeStatistics();
            List<WorkerStatistics> workerStatistics = saturation.getWorkerStatistics();

            if (roundNumber > numberOfWarmUpRounds) {
                runtimeInMSPerRound.add((double) (controlNodeStatistics.getWorkerInitializationTimeMS()
                        + controlNodeStatistics.getTotalSaturationTimeMS()));

                runtimeInMSPerRoundWithSendingClosure.add((double) (
                        controlNodeStatistics.getWorkerInitializationTimeMS()
                                + controlNodeStatistics.getTotalSaturationTimeMS()
                                + controlNodeStatistics.getCollectingClosureResultsFromWorkersMS()
                ));

                createStatisticsCSVFiles("parallel", controlNodeStatistics,
                        workerStatistics);
            }
        }
        return new RuntimeMeasurements(
                new DescriptiveStatistics(runtimeInMSPerRoundWithSendingClosure.stream().mapToDouble(d -> d).toArray()),
                new DescriptiveStatistics(runtimeInMSPerRound.stream().mapToDouble(d -> d).toArray())
        );
    }

    public void createStatisticsCSVFiles(String approach, ControlNodeStatistics controlNodeStatistics,
                                         List<WorkerStatistics> workerStatistics) {
        String csvControlNodeStatsPath = Paths.get(this.outputDirectory.toString(),
                approach + "_controlNode"
                        + "_" + benchmarkType
                        + "_numMessages=" + numberOfInitialAxioms
                        + "_numWorkers=" + workers.size())
                + ".csv";

        String csvWorkerStatsPath = Paths.get(this.outputDirectory.toString(),
                approach + "_workers"
                        + "_" + benchmarkType
                        + "_numMessages=" + numberOfInitialAxioms
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

    private void createCSVFile() throws IOException {
        CSVFormat csvFormat = CSVFormat.Builder.create()
                .setHeader(this.csvHeader.toArray(new String[csvHeader.size()]))
                .setDelimiter(";")
                .build();
        FileWriter out = new FileWriter(
                Paths.get(outputDirectory.toString(), benchmarkType + "_" + "benchmark.csv").toFile());
        try (CSVPrinter printer = new CSVPrinter(out, csvFormat)) {
            for (List<String> values : this.csvRows) {
                printer.printRecord(values);
            }
        }
    }

    private class RuntimeMeasurements {
        DescriptiveStatistics runtimeWithSendingClosureResults;
        DescriptiveStatistics runtimeWithoutSendingClosureResults;

        public RuntimeMeasurements(DescriptiveStatistics runtimeWithSendingClosureResults,
                                   DescriptiveStatistics runtimeWithoutSendingClosureResults) {
            this.runtimeWithSendingClosureResults = runtimeWithSendingClosureResults;
            this.runtimeWithoutSendingClosureResults = runtimeWithoutSendingClosureResults;
        }

        public DescriptiveStatistics getRuntimeWithSendingClosureResults() {
            return runtimeWithSendingClosureResults;
        }

        public DescriptiveStatistics getRuntimeWithoutSendingClosureResults() {
            return runtimeWithoutSendingClosureResults;
        }
    }

    private class CSVRow {
        String benchmarkType = SaturationBenchmark.this.benchmarkType;
        String approach;
        MessageDistributionType messageDistributionType;
        boolean withSendingClosureResults;
        Long numberOfInitialAxioms;
        Integer numWorkers;
        Double minRuntimeMS;
        Double maxRuntimeMS;
        Double averageRuntimeMS;


        public CSVRow(String approach, Long numberOfInitialAxioms, MessageDistributionType messageDistributionType,
                      boolean withSendingClosureResults,
                      Integer numWorkers,
                      Double minRuntimeMS, Double maxRuntimeMS, Double averageRuntimeMS) {
            this.approach = approach;
            this.messageDistributionType = messageDistributionType;
            this.withSendingClosureResults = withSendingClosureResults;
            this.numberOfInitialAxioms = numberOfInitialAxioms;
            this.numWorkers = numWorkers;
            this.minRuntimeMS = minRuntimeMS;
            this.maxRuntimeMS = maxRuntimeMS;
            this.averageRuntimeMS = averageRuntimeMS;
        }

        protected List<String> toCSVRow() {
            List<String> row = new ArrayList<>();
            row.add(benchmarkType);
            row.add(approach);
            row.add(messageDistributionType.toString().toLowerCase());
            row.add(String.valueOf(withSendingClosureResults));
            row.add(numberOfInitialAxioms + "");
            row.add(numWorkers + "");
            row.add(minRuntimeMS + "");
            row.add(maxRuntimeMS + "");
            row.add(averageRuntimeMS + "");

            assert row.size() == SaturationBenchmark.this.csvHeader.size();
            return row;
        }
    }

}

