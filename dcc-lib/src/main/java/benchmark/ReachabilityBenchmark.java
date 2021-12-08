package benchmark;

import benchmark.graphgeneration.ReachabilityBinaryTreeGenerator;
import com.google.common.base.Stopwatch;
import networking.ServerData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.roaringbitmap.RoaringBitmap;
import reasoning.saturation.SingleThreadedSaturation;
import reasoning.saturation.distributed.DistributedSaturation;
import reasoning.saturation.distributed.SaturationWorker;
import reasoning.saturation.distributed.communication.BenchmarkConfiguration;
import reasoning.saturation.models.DistributedWorkerModel;
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

    private List<Integer> binaryTreeDepth;
    private List<Integer> numberOfWorkers;
    private List<Double> mbitsPerSecondNetworkBandwidth;
    private int numberOfExperimentRepetitions = 3;
    private ArrayList<Double> runtimeInMSPerRound = new ArrayList<>();
    private String[] csvHeader = {"depth", "nodes", "#nonTransitiveEdges", "#transitiveEdges", "#totalEdges", "#workers", "bandwidthMbits", "minRuntimeMS", "maxRuntimeMS", "averageRuntimeMS"};
    private List<List<String>> statistics = new ArrayList<>();
    private Stopwatch stopwatch;
    private File outputDirectory;

    {
        binaryTreeDepth = new ArrayList<>();
        binaryTreeDepth.add(5);
        binaryTreeDepth.add(10);
        binaryTreeDepth.add(15);
        binaryTreeDepth.add(20);

        // BWUniCluster has maximum of 40 threads - 2 threads per worker, thus, 20 threads at max.
        numberOfWorkers = new ArrayList<>();
        numberOfWorkers.add(1);
        numberOfWorkers.add(5);
        numberOfWorkers.add(10);
        //numberOfWorkers.add(15);
        //numberOfWorkers.add(20);

        mbitsPerSecondNetworkBandwidth = new ArrayList<>();
        mbitsPerSecondNetworkBandwidth.add(10d);
        mbitsPerSecondNetworkBandwidth.add(100d);
        mbitsPerSecondNetworkBandwidth.add(200d);
        mbitsPerSecondNetworkBandwidth.add(2000d);
    }

    public ReachabilityBenchmark(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        this.outputDirectory.mkdir();
    }

    public static void main(String[] args) {
        File outputDirectory;
        if (args.length > 0) {
            outputDirectory = new File(args[0]);
        } else {
            outputDirectory = new File(System.getProperty("user.dir"));
        }

        ReachabilityBenchmark benchmark = new ReachabilityBenchmark(outputDirectory);
        benchmark.startBenchmark();

    }

    public void startBenchmark() {
        log.info("Starting experiment...");
        log.info(ConsoleUtils.getSeparator());

        for (Integer treeDepth : binaryTreeDepth) {
            ReachabilityBinaryTreeGenerator generator = new ReachabilityBinaryTreeGenerator(treeDepth);
            List<ToldReachability> initialAxioms = generator.generateGraph();


            for (Integer numWorkers : numberOfWorkers) {
                for (Double mbits : mbitsPerSecondNetworkBandwidth) {
                    if (numWorkers == 1 && mbits < 2000) {
                        // perform single threaded computation only one time
                        continue;
                    }

                    for (int i = 0; i < numberOfExperimentRepetitions; i++) {
                        log.info("Round " + i);
                        log.info("Tree Depth: " + treeDepth);
                        log.info("#Workers: " + numWorkers);
                        log.info("Mbits bandwidth: " + mbits + "[Mbits]");
                        log.info(ConsoleUtils.getSeparator());
                        if (numWorkers == 1) {
                            singleThreadedClosureComputation(initialAxioms);
                        } else {
                            distributedClosureComputation(initialAxioms, numWorkers, mbits);
                        }
                    }
                    DescriptiveStatistics runtimeInMSStats = new DescriptiveStatistics(this.runtimeInMSPerRound.stream().mapToDouble(d -> d).toArray());
                    runtimeInMSPerRound.clear();

                    List<String> row = new ArrayList<>();

                    row.add(treeDepth + "");
                    row.add(generator.getNumberOfNodes() + "");
                    row.add(generator.getNumberOfEdgesOriginalGraph() + "");
                    row.add(generator.getNumberOfEdgesInTransitiveClosure() + "");
                    row.add(generator.getTotalNumberOfEdges() + "");
                    row.add(numWorkers + "");
                    row.add(mbits + "");
                    row.add(runtimeInMSStats.getMin() + "");
                    row.add(runtimeInMSStats.getMax() + "");
                    row.add(runtimeInMSStats.getMean() + "");

                    statistics.add(row);

                }
            }
        }

        try {
            createCSVFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void distributedClosureComputation(List<ToldReachability> initialAxioms, int numWorkers, double mbits) {


        BenchmarkConfiguration benchmarkConfiguration = new BenchmarkConfiguration(mbits);

        // initialize workers
        SaturationWorkerServerGenerator<ReachabilityClosure, Reachability, RoaringBitmap> workerServerFactory;
        workerServerFactory = new SaturationWorkerServerGenerator<>(benchmarkConfiguration, numWorkers, new Callable<>() {
            @Override
            public ReachabilityClosure call() throws Exception {
                return new ReachabilityClosure();
            }
        });

        List<SaturationWorker<ReachabilityClosure, Reachability, RoaringBitmap>> saturationWorkers;
        saturationWorkers = workerServerFactory.generateWorkers();
        List<Thread> threads = saturationWorkers.stream().map(Thread::new).collect(Collectors.toList());
        threads.forEach(Thread::start);


        // initialize control node
        List<ServerData> serverDataList = workerServerFactory.getServerDataList();
        ReachabilityWorkerFactory workerFactory = new ReachabilityWorkerFactory(
                initialAxioms,
                serverDataList
        );

        List<DistributedWorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> workers = workerFactory.generateDistributedWorkers();
        DistributedSaturation<ReachabilityClosure, Reachability, RoaringBitmap> saturation = new DistributedSaturation<>(
                benchmarkConfiguration,
                workers,
                new ReachabilityWorkloadDistributor(workers),
                initialAxioms,
                new ReachabilityClosure()
        );

        // run saturation
        this.stopwatch = Stopwatch.createStarted();
        ReachabilityClosure closure = saturation.saturate();
        assert closure.getClosureResults().size() > 0;

        Duration runtime = this.stopwatch.elapsed();
        this.runtimeInMSPerRound.add((double) runtime.toMillis());

        saturationWorkers.forEach(SaturationWorker::terminate);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void singleThreadedClosureComputation(List<? extends Reachability> initialAxioms) {
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
        this.runtimeInMSPerRound.add((double) runtime.toMillis());

        assert closure.getClosureResults().size() > 0;
    }


    public void createCSVFile() throws IOException {
        CSVFormat csvFormat = CSVFormat.Builder.create()
                .setHeader(this.csvHeader)
                .setDelimiter(";")
                .build();
        FileWriter out = new FileWriter("binaryTreeBenchmark.csv");
        try (CSVPrinter printer = new CSVPrinter(out, csvFormat)) {
            for (List<String> values : this.statistics) {
                printer.printRecord(values);
            }
        }
    }

}
