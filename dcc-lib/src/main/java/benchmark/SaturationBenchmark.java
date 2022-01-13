package benchmark;


import benchmark.workergeneration.SaturationDockerWorkerGenerator;
import benchmark.workergeneration.SaturationJVMWorkerGenerator;
import benchmark.workergeneration.SaturationWorkerGenerator;
import benchmark.workergeneration.SaturationWorkerThreadGenerator;
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

    private int numberOfExperimentRepetitions = 2;
    private int numberOfWarmUpRounds = 2;
    private Set<SaturationApproach> includedApproaches;
    private Stopwatch stopwatch;
    private File outputDirectory;
    private String benchmarkType;

    private List<List<String>> csvRows = new ArrayList<>();
    private List<String> csvHeader;

    private List<Integer> numberOfThreadsForSingleDistributedWorker = Collections.singletonList(2);

    private boolean workerNodeStatistics = false;

    private SaturationInitializationFactory<C, A, T> initializationFactory;
    private List<? extends A> initialAxioms;
    private List<WorkerModel<C, A, T>> workers;

    private SaturationWorkerGenerator workerGenerator;


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
                               int numberOfExperimentRepetitions,
                               boolean workerNodeStatistics,
                               List<Integer> numberOfThreadsForSingleDistributedWorker) {
        this.benchmarkType = benchmarkType;
        this.numberOfThreadsForSingleDistributedWorker = numberOfThreadsForSingleDistributedWorker;
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

            for (Integer numThreadsForSingleWorker : numberOfThreadsForSingleDistributedWorker) {
                // distributed - each worker in separate thread
                if (includedApproaches.contains(SaturationApproach.DISTRIBUTED_MULTITHREADED)) {
                    runDistributedSaturationBenchmark(SaturationApproach.DISTRIBUTED_MULTITHREADED, numThreadsForSingleWorker);
                    initializationFactory.resetFactory();
                }

                // distributed - each worker in separate JVM
                if (includedApproaches.contains(SaturationApproach.DISTRIBUTED_SEPARATE_JVM)) {
                    runDistributedSaturationBenchmark(SaturationApproach.DISTRIBUTED_SEPARATE_JVM, numThreadsForSingleWorker);
                    initializationFactory.resetFactory();
                }

                // distributed - each worker in separate docker container
                if (includedApproaches.contains(SaturationApproach.DISTRIBUTED_SEPARATE_DOCKER_CONTAINER)) {
                    runDistributedSaturationBenchmark(SaturationApproach.DISTRIBUTED_SEPARATE_DOCKER_CONTAINER, numThreadsForSingleWorker);
                    initializationFactory.resetFactory();
                }

                // distributed - worker have been already started in separate docker container, only control node is started
                if (includedApproaches.contains(SaturationApproach.DISTRIBUTED_DOCKER_BENCHMARK)) {
                    runDistributedSaturationBenchmark(SaturationApproach.DISTRIBUTED_DOCKER_BENCHMARK, numThreadsForSingleWorker);
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

    public void runDistributedSaturationBenchmark(SaturationApproach distributedApproach, int numberOfThreadsForSingleDistributedWorker) {
        DescriptiveStatistics runtime = null;
        log.info("Distributed");
        log.info("# Initial Axioms: " + initialAxioms.size());
        log.info("# Workers: " + workers.size());
        log.info("Approach: " + distributedApproach.toString());
        try {
            runtime = distributedClosureComputation(distributedApproach, numberOfThreadsForSingleDistributedWorker);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        CSVRow row = new CSVRow(
                distributedApproach.toString().toLowerCase(),
                initialAxioms.size(),
                workers.size(),
                runtime.getMin(),
                runtime.getMax(),
                runtime.getMean()
        );

        csvRows.add(row.toCSVRow());

        log.info(ConsoleUtils.getSeparator());

    }

    private DescriptiveStatistics distributedClosureComputation(SaturationApproach distributedApproach,
                                                                int numberOfThreadsForSingleDistributedWorker) throws
            InterruptedException {
        List<Double> runtimeInMSPerRound = new ArrayList<>();
        List<ServerData> serverDataList = null;

        // initialize workers
        switch (distributedApproach) {
            case DISTRIBUTED_MULTITHREADED:
                workerGenerator = new SaturationWorkerThreadGenerator(workers.size(), numberOfThreadsForSingleDistributedWorker);
                break;
            case DISTRIBUTED_SEPARATE_DOCKER_CONTAINER:
                workerGenerator = new SaturationDockerWorkerGenerator(workers.size());
                break;
            case DISTRIBUTED_SEPARATE_JVM:
                workerGenerator = new SaturationJVMWorkerGenerator(workers.size(), numberOfThreadsForSingleDistributedWorker);
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
                    new SaturationConfiguration(true, workerNodeStatistics),
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
            }

            // distributed saturation stats
            createStatisticsCSVFiles(distributedApproach.toString().toLowerCase(), controlNodeStatistics,
                    workerStatistics);
        }

        // stop workers
        if (!distributedApproach.equals(SaturationApproach.DISTRIBUTED_DOCKER_BENCHMARK)) {
            workerGenerator.stopWorkers();
        }

        return new DescriptiveStatistics(runtimeInMSPerRound.stream().mapToDouble(d -> d).toArray());
    }


    private DescriptiveStatistics singleThreadedClosureComputation() {
        List<Double> runtimeInMSPerRound = new ArrayList<>();

        for (int roundNumber = 1; roundNumber <= this.numberOfWarmUpRounds + this.numberOfExperimentRepetitions; roundNumber++) {
            if (roundNumber <= numberOfWarmUpRounds) {
                log.info("Warm-up Round " + roundNumber);
            } else {
                log.info("Round " + (roundNumber - numberOfWarmUpRounds));
            }


            SingleThreadedSaturation<C, A> saturation = new SingleThreadedSaturation<>(
                    initialAxioms.iterator(),
                    initializationFactory.generateRules(),
                    initializationFactory.getNewClosure()
            );

            // run saturation
            this.stopwatch = Stopwatch.createStarted();
            C closure = saturation.saturate();
            assert closure.getClosureResults().size() > 0;

            if (roundNumber > numberOfWarmUpRounds) {
                Duration runtime = this.stopwatch.elapsed();
                runtimeInMSPerRound.add((double) runtime.toMillis());
            }

            assert closure.getClosureResults().size() > 0;

        }
        return new DescriptiveStatistics(runtimeInMSPerRound.stream().mapToDouble(d -> d).toArray());
    }

    private DescriptiveStatistics parallelClosureComputation() {
        List<Double> runtimeInMSPerRound = new ArrayList<>();

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

                createStatisticsCSVFiles("parallel", controlNodeStatistics,
                        workerStatistics);
            }
        }
        return new DescriptiveStatistics(runtimeInMSPerRound.stream().mapToDouble(d -> d).toArray());
    }

    public void createStatisticsCSVFiles(String approach, ControlNodeStatistics controlNodeStatistics,
                                         List<WorkerStatistics> workerStatistics) {
        String csvControlNodeStatsPath = Paths.get(this.outputDirectory.toString(),
                approach + "_controlNode"
                        + "_" + benchmarkType
                        + "_numMessages=" + initialAxioms.size()
                        + "_numWorkers=" + workers.size())
                + ".csv";

        String csvWorkerStatsPath = Paths.get(this.outputDirectory.toString(),
                approach + "_workers"
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

