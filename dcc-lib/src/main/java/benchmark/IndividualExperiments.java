package benchmark;

import benchmark.echoclosure.EchoAxiom;
import benchmark.echoclosure.EchoClosure;
import benchmark.echoclosure.EchoSaturationInitializationFactory;
import benchmark.graphgeneration.ReachabilityBinaryTreeGenerator;
import benchmark.graphgeneration.ReachabilityChainGraphGenerator;
import benchmark.transitiveclosure.Reachability;
import benchmark.transitiveclosure.ReachabilityClosure;
import benchmark.transitiveclosure.ReachabilitySaturationInitializationFactory;
import benchmark.transitiveclosure.ToldReachability;
import enums.SaturationApproach;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.roaringbitmap.RoaringBitmap;

import java.io.File;
import java.util.*;

public class IndividualExperiments {

    private static final boolean collectWorkerNodeStatistics = true;

    private static final int EXPERIMENT_ROUNDS = 1;
    private static final int WARM_UP_ROUNDS = 1;

    private static List<Integer> binaryTreeDepthList;
    private static List<Integer> initialEchoAxioms;
    private static List<Integer> chainDepthList;
    private static List<Integer> numberOfWorkersList;
    private static List<Integer> numberOfThreadsPerDistributedWorker;

    static {
        binaryTreeDepthList = new ArrayList<>();
        binaryTreeDepthList.add(16);

        initialEchoAxioms = new ArrayList<>();
        //initialEchoAxioms.add(10_000);
        //initialEchoAxioms.add(20_000);
        //initialEchoAxioms.add(50_000);
        //initialEchoAxioms.add(100_000);
        //initialEchoAxioms.add(200_000);
        //initialEchoAxioms.add(500_000);
        //initialEchoAxioms.add(1_000_000);
        //initialEchoAxioms.add(10_000_000);

        chainDepthList = new ArrayList<>();
        for (int i = 100; i <= 1000; i += 100) {
            chainDepthList.add(i);
        }

        numberOfWorkersList = new ArrayList<>();
        numberOfWorkersList.add(1);
        //numberOfWorkersList.add(2);
        numberOfWorkersList.add(4);

        numberOfThreadsPerDistributedWorker = new ArrayList<>();
        numberOfThreadsPerDistributedWorker.add(1);
    }

    public static void main(String[] args) {
        /*
        Scanner scanner = new Scanner(System.in);
        System.out.println("Press ENTER to start benchmark...");
        scanner.nextLine();
         */

        File outputDirectory;
        if (args.length > 0) {
            outputDirectory = new File(args[0]);
        } else {
            outputDirectory = new File(System.getProperty("user.dir"));
        }

        benchmark(outputDirectory);
    }

    public static void benchmark(File outputDirectory) {
        Set<SaturationApproach> includedApproaches = new UnifiedSet<>();
        includedApproaches.add(SaturationApproach.SINGLE_THREADED);
        includedApproaches.add(SaturationApproach.PARALLEL);
        includedApproaches.add(SaturationApproach.DISTRIBUTED_MULTITHREADED);
        //includedApproaches.add(SaturationApproach.DISTRIBUTED_SEPARATE_JVM);
        //includedApproaches.add(SaturationApproach.DISTRIBUTED_SEPARATE_DOCKER_CONTAINER);

        binaryTreeBenchmark(outputDirectory, includedApproaches, binaryTreeDepthList, numberOfWorkersList);
        //chainGraphBenchmark(outputDirectory, includedApproaches, chainDepthList, numberOfWorkersList);
        //echoBenchmark(outputDirectory, includedApproaches, initialEchoAxioms, numberOfWorkersList);
    }

    public static void binaryTreeBenchmark(File outputDirectory,
                                           Set<SaturationApproach> includedApproaches,
                                           List<Integer> binaryTreeDepthList,
                                           List<Integer> numberOfWorkersList) {

        SaturationBenchmark<ReachabilityClosure, Reachability, RoaringBitmap> binaryTreeBenchmark = new SaturationBenchmark<>(
                "BinaryTree",
                includedApproaches,
                outputDirectory,
                WARM_UP_ROUNDS,
                EXPERIMENT_ROUNDS,
                collectWorkerNodeStatistics,
                numberOfThreadsPerDistributedWorker
        );
        for (Integer depth : binaryTreeDepthList) {
            ReachabilityBinaryTreeGenerator treeGenerator = new ReachabilityBinaryTreeGenerator(depth);
            List<ToldReachability> initialAxioms = treeGenerator.generateGraph();
            for (Integer numWorkers : numberOfWorkersList) {
                ReachabilitySaturationInitializationFactory factory = new ReachabilitySaturationInitializationFactory(
                        initialAxioms,
                        numWorkers,
                        0
                );
                binaryTreeBenchmark.startBenchmark(factory);
            }
        }
        binaryTreeBenchmark.finishBenchmark();

    }

    public static void chainGraphBenchmark(File outputDirectory,
                                           Set<SaturationApproach> includedApproaches,
                                           List<Integer> chainDepthList,
                                           List<Integer> numberOfWorkersList) {

        SaturationBenchmark<ReachabilityClosure, Reachability, RoaringBitmap> chainGraphBenchmark = new SaturationBenchmark<>(
                "ChainGraph",
                includedApproaches,
                outputDirectory,
                WARM_UP_ROUNDS,
                EXPERIMENT_ROUNDS,
                collectWorkerNodeStatistics,
                numberOfThreadsPerDistributedWorker
        );
        for (Integer depth : chainDepthList) {
            ReachabilityChainGraphGenerator treeGenerator = new ReachabilityChainGraphGenerator(depth);
            List<ToldReachability> initialAxioms = treeGenerator.generateGraph();
            for (Integer numWorkers : numberOfWorkersList) {
                ReachabilitySaturationInitializationFactory factory = new ReachabilitySaturationInitializationFactory(
                        initialAxioms,
                        numWorkers,
                        0
                );
                chainGraphBenchmark.startBenchmark(factory);
            }
        }
        chainGraphBenchmark.finishBenchmark();
    }

    public static void echoBenchmark(File outputDirectory,
                                     Set<SaturationApproach> includedApproaches,
                                     List<Integer> initialMessagesList,
                                     List<Integer> numberOfWorkersList) {
        SaturationBenchmark<EchoClosure, EchoAxiom, Integer> echoBenchmark = new SaturationBenchmark<>(
                "Echo",
                includedApproaches,
                outputDirectory,
                WARM_UP_ROUNDS,
                EXPERIMENT_ROUNDS,
                collectWorkerNodeStatistics,
                numberOfThreadsPerDistributedWorker
        );
        for (Integer initialMessages : initialMessagesList) {
            for (Integer numWorkers : numberOfWorkersList) {
                EchoSaturationInitializationFactory factory = new EchoSaturationInitializationFactory(
                        numWorkers,
                        initialMessages
                );
                echoBenchmark.startBenchmark(factory);
            }
        }
        echoBenchmark.finishBenchmark();
    }

}
