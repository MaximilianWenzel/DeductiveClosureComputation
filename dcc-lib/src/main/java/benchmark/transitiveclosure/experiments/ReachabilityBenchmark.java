package benchmark.transitiveclosure.experiments;

import benchmark.graphgeneration.BinaryTreeGenerator;
import benchmark.graphgeneration.GraphGenerator;
import benchmark.graphgeneration.ReachabilityBinaryTreeGenerator;
import benchmark.transitiveclosure.*;
import com.google.common.base.Stopwatch;
import networking.ServerData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.roaringbitmap.RoaringBitmap;
import reasoning.saturation.SingleThreadedSaturation;
import reasoning.saturation.distributed.DistributedSaturation;
import reasoning.saturation.distributed.SaturationWorker;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.parallel.ParallelSaturation;
import util.ConsoleUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ReachabilityBenchmark {
    private Logger log = ConsoleUtils.getLogger();

    private int numberOfExperimentRepetitions = 1;
    private List<String> csvHeader;
    private List<List<String>> csvRows = new ArrayList<>();
    private Stopwatch stopwatch;
    private File outputDirectory;

    {
        csvHeader = new ArrayList<>();
        csvHeader.add("graphType");
        csvHeader.add("approach");
        csvHeader.add("depth");
        csvHeader.add("nodes");
        csvHeader.add("numNonTransitiveEdges");
        csvHeader.add("numTransitiveEdges");
        csvHeader.add("numTotalEdges");
        csvHeader.add("numWorkers");
        csvHeader.add("numAxiomsToBuffer");
        csvHeader.add("minRuntimeMS");
        csvHeader.add("maxRuntimeMS");
        csvHeader.add("averageRuntimeMS");
    }


    public ReachabilityBenchmark(File outputDirectory, int numberOfExperimentRepetitions) {
        this.outputDirectory = outputDirectory;
        this.outputDirectory.mkdir();
        this.numberOfExperimentRepetitions = numberOfExperimentRepetitions;
    }

    public void startBinaryTreeBenchmark(List<Integer> treeDepth,
                                         List<Integer> numberOfWorkers,
                                         List<Integer> maxNumberOfAxiomsToBuffer) {
        for (Integer depth : treeDepth) {
            BinaryTreeGenerator<ToldReachability> generator = new ReachabilityBinaryTreeGenerator(depth);
            startBenchmark(generator, numberOfWorkers, maxNumberOfAxiomsToBuffer);
        }
    }

    private void startBenchmark(GraphGenerator<ToldReachability> generator,
                                List<Integer> numberOfWorkers,
                                List<Integer> maxNumberOfAxiomsToBuffer) {
        log.info(ConsoleUtils.getSeparator());
        log.info("Starting experiment...");
        log.info(ConsoleUtils.getSeparator());

        List<ToldReachability> initialAxioms = generator.generateGraph();
        // single threaded
        runSingleThreadedSaturationBenchmark(initialAxioms, generator);

        for (Integer numWorkers : numberOfWorkers) {
            // parallel
            runParallelSaturationBenchmark(initialAxioms, generator, numWorkers);

            for (Integer numAxiomsToBuffer : maxNumberOfAxiomsToBuffer) {
                // distributed
                runDistributedSaturationBenchmark(initialAxioms, generator, numWorkers, numAxiomsToBuffer);
            }
        }

        try {
            createCSVFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runSingleThreadedSaturationBenchmark(List<ToldReachability> initialAxioms, GraphGenerator<ToldReachability> generator) {
        log.info("Single");
        log.info("Tree Depth: " + generator.getDiameter());

        DescriptiveStatistics runtimeInMSStats = singleThreadedClosureComputation(initialAxioms);

        CSVRow row = new CSVRow(
                generator.getGraphTypeName(),
                "single",
                generator.getDiameter(),
                generator.getNumberOfNodes(),
                generator.getNumberOfEdgesOriginalGraph(),
                generator.getNumberOfEdgesInTransitiveClosure(),
                generator.getTotalNumberOfEdges(),
                null,
                null,
                runtimeInMSStats.getMin(),
                runtimeInMSStats.getMax(),
                runtimeInMSStats.getMean()
        );
        this.csvRows.add(row.toCSVRow());

        log.info(ConsoleUtils.getSeparator());

    }

    public void runParallelSaturationBenchmark(List<ToldReachability> initialAxioms, GraphGenerator<ToldReachability> generator, int numWorkers) {
        log.info("Parallel");
        log.info("Tree Depth: " + generator.getDiameter());
        log.info("#Workers: " + numWorkers);

        DescriptiveStatistics runtimeInMSStats = parallelClosureComputation(initialAxioms, numWorkers);

        CSVRow row = new CSVRow(
                generator.getGraphTypeName(),
                "parallel",
                generator.getDiameter(),
                generator.getNumberOfNodes(),
                generator.getNumberOfEdgesOriginalGraph(),
                generator.getNumberOfEdgesInTransitiveClosure(),
                generator.getTotalNumberOfEdges(),
                numWorkers,
                null,
                runtimeInMSStats.getMin(),
                runtimeInMSStats.getMax(),
                runtimeInMSStats.getMean()
        );

        this.csvRows.add(row.toCSVRow());

        log.info(ConsoleUtils.getSeparator());

    }

    public void runDistributedSaturationBenchmark(List<ToldReachability> initialAxioms, GraphGenerator<ToldReachability> generator, int numWorkers, int numAxiomsToBuffer) {
        DescriptiveStatistics runtime = null;
        log.info("Distributed");
        log.info("Tree Depth: " + generator.getDiameter());
        log.info("#Workers: " + numWorkers);
        runtime = distributedClosureComputation(initialAxioms, numWorkers, numAxiomsToBuffer);

        CSVRow row = new CSVRow(
                generator.getGraphTypeName(),
                "distributed",
                generator.getDiameter(),
                generator.getNumberOfNodes(),
                generator.getNumberOfEdgesOriginalGraph(),
                generator.getNumberOfEdgesInTransitiveClosure(),
                generator.getTotalNumberOfEdges(),
                numWorkers,
                numAxiomsToBuffer,
                runtime.getMin(),
                runtime.getMax(),
                runtime.getMean()
        );

        csvRows.add(row.toCSVRow());

        log.info(ConsoleUtils.getSeparator());

    }

    private DescriptiveStatistics distributedClosureComputation(List<ToldReachability> initialAxioms, int numWorkers, int numberOfAxiomsToBuffer) {
        List<Double> runtimeInMSPerRound = new ArrayList<>();

        for (int i = 1; i <= this.numberOfExperimentRepetitions; i++) {
            log.info("Round " + i);


            // initialize workers
            SaturationWorkerServerGenerator<ReachabilityClosure, Reachability, RoaringBitmap> workerServerFactory;

            workerServerFactory = new SaturationWorkerServerGenerator<>(null, numWorkers, numberOfAxiomsToBuffer,
                    (Callable) () -> new ReachabilityClosure());

            List<SaturationWorker<ReachabilityClosure, Reachability, RoaringBitmap>> saturationWorkers;
            saturationWorkers = workerServerFactory.generateWorkers();
            List<Thread> threads = saturationWorkers.stream().map(Thread::new).collect(Collectors.toList());
            threads.forEach(Thread::start);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            // initialize control node
            List<ServerData> serverDataList = workerServerFactory.getServerDataList();
            ReachabilityWorkerFactory workerFactory = new ReachabilityWorkerFactory(
                    initialAxioms,
                    serverDataList
            );

            List<DistributedWorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> workers = workerFactory.generateDistributedWorkers();
            DistributedSaturation<ReachabilityClosure, Reachability, RoaringBitmap> saturation = new DistributedSaturation<>(
                    null,
                    workers,
                    new ReachabilityWorkloadDistributor(workers),
                    initialAxioms,
                    new ReachabilityClosure(),
                    numberOfAxiomsToBuffer
            );

            // run saturation
            this.stopwatch = Stopwatch.createStarted();
            ReachabilityClosure closure = saturation.saturate();
            assert closure.getClosureResults().size() > 0;

            Duration runtime = this.stopwatch.elapsed();
            runtimeInMSPerRound.add((double) runtime.toMillis());

            saturationWorkers.forEach(SaturationWorker::terminate);
        }

        return new DescriptiveStatistics(runtimeInMSPerRound.stream().mapToDouble(d -> d).toArray());
    }

    private DescriptiveStatistics singleThreadedClosureComputation(List<? extends Reachability> initialAxioms) {
        List<Double> runtimeInMSPerRound = new ArrayList<>();

        for (int i = 1; i <= this.numberOfExperimentRepetitions; i++) {
            log.info("Round " + i);

            SingleThreadedSaturation<ReachabilityClosure, Reachability> saturation = new SingleThreadedSaturation<>(
                    initialAxioms.iterator(),
                    ReachabilityWorkerFactory.getReachabilityRules(),
                    new ReachabilityClosure()
            );

            // run saturation
            this.stopwatch = Stopwatch.createStarted();
            ReachabilityClosure closure = saturation.saturate();
            assert closure.getClosureResults().size() > 0;

            Duration runtime = this.stopwatch.elapsed();
            runtimeInMSPerRound.add((double) runtime.toMillis());

            assert closure.getClosureResults().size() > 0;

        }
        return new DescriptiveStatistics(runtimeInMSPerRound.stream().mapToDouble(d -> d).toArray());
    }

    private DescriptiveStatistics parallelClosureComputation(List<? extends Reachability> initialAxioms, int numberOfWorkers) {
        List<Double> runtimeInMSPerRound = new ArrayList<>();

        for (int i = 1; i <= this.numberOfExperimentRepetitions; i++) {
            log.info("Round " + i);

            ParallelSaturation<ReachabilityClosure, Reachability, RoaringBitmap> saturation = new ParallelSaturation<>(
                    new ReachabilitySaturationInitializationFactory(initialAxioms, numberOfWorkers)
            );

            // run saturation
            this.stopwatch = Stopwatch.createStarted();
            ReachabilityClosure closure = saturation.saturate();
            Duration runtime = this.stopwatch.elapsed();
            assert closure.getClosureResults().size() > 0;
            runtimeInMSPerRound.add((double) runtime.toMillis());
        }
        return new DescriptiveStatistics(runtimeInMSPerRound.stream().mapToDouble(d -> d).toArray());
    }

    public void createCSVFile() throws IOException {
        CSVFormat csvFormat = CSVFormat.Builder.create()
                .setHeader(this.csvHeader.toArray(new String[csvHeader.size()]))
                .setDelimiter(";")
                .build();
        FileWriter out = new FileWriter("transitiveClosureBenchmark.csv");
        try (CSVPrinter printer = new CSVPrinter(out, csvFormat)) {
            for (List<String> values : this.csvRows) {
                printer.printRecord(values);
            }
        }
    }

    private class CSVRow {
        String graphType;
        String approach;
        Integer depth;
        Integer nodes;
        Integer numNonTransitiveEdge;
        Integer numTransitiveEdges;
        Integer numTotalEdges;
        Integer numWorkers;
        Integer numAxiomsToBuffer;
        Double minRuntimeMS;
        Double maxRuntimeMS;
        Double averageRuntimeMS;

        public CSVRow(String graphType, String approach, Integer depth, Integer nodes, Integer numNonTransitiveEdge, Integer numTransitiveEdges,
                      Integer numTotalEdges, Integer numWorkers, Integer numAxiomsToBuffer,
                      Double minRuntimeMS, Double maxRuntimeMS, Double averageRuntimeMS) {


            this.graphType = graphType;
            this.approach = approach;
            this.depth = depth;
            this.nodes = nodes;
            this.numNonTransitiveEdge = numNonTransitiveEdge;
            this.numTransitiveEdges = numTransitiveEdges;
            this.numTotalEdges = numTotalEdges;
            this.numWorkers = numWorkers;
            this.numAxiomsToBuffer = numAxiomsToBuffer;
            this.minRuntimeMS = minRuntimeMS;
            this.maxRuntimeMS = maxRuntimeMS;
            this.averageRuntimeMS = averageRuntimeMS;
        }

        protected List<String> toCSVRow() {
            List<String> row = new ArrayList<>();
            row.add(graphType);
            row.add(approach);
            row.add(depth + "");
            row.add(nodes + "");
            row.add(numNonTransitiveEdge + "");
            row.add(numTransitiveEdges + "");
            row.add(numTotalEdges + "");
            row.add(numWorkers + "");
            row.add(numAxiomsToBuffer + "");
            row.add(minRuntimeMS + "");
            row.add(maxRuntimeMS + "");
            row.add(averageRuntimeMS + "");

            assert row.size() == csvHeader.size();
            return row;
        }
    }
}
