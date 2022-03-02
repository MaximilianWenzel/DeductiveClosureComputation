package benchmark;

import benchmark.echoclosure.EchoAxiom;
import benchmark.echoclosure.EchoClosure;
import benchmark.echoclosure.EchoSaturationInitializationFactory;
import benchmark.graphgeneration.ReachabilityBinaryTreeGenerator;
import benchmark.graphgeneration.ReachabilityChainGraphGenerator;
import benchmark.graphgeneration.ReachabilityRandomDigraphGenerator;
import benchmark.transitiveclosure.Reachability;
import benchmark.transitiveclosure.ReachabilityClosure;
import benchmark.transitiveclosure.ReachabilitySaturationInitializationFactory;
import benchmark.transitiveclosure.ToldReachability;
import enums.MessageDistributionType;
import enums.SaturationApproach;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.io.File;
import java.util.List;
import java.util.Set;

public class IndividualExperiments {

    private static final boolean collectWorkerNodeStatistics = false;

    private static final int EXPERIMENT_ROUNDS = 1;
    private static final int WARM_UP_ROUNDS = 1;

    private static Set<Integer> binaryTreeDepthList;
    private static Set<Integer> randomDigraphNodes;
    private static Set<Integer> initialEchoAxioms;
    private static Set<Integer> chainDepthList;
    private static Set<Integer> numberOfWorkersList;
    private static Set<MessageDistributionType> messageDistributionTypes;

    static {
        randomDigraphNodes = new UnifiedSet<>();
        //randomDigraphNodes.add(100);
        //randomDigraphNodes.add(480);
        randomDigraphNodes.add(512);
        //randomDigraphNodes.add(960);
        //randomDigraphNodes.add(1024);
        //randomDigraphNodes.add(1920);
        //randomDigraphNodes.add(2048);
        //randomDigraphNodes.add(4096);
        //randomDigraphNodes.add(6144);

        binaryTreeDepthList = new UnifiedSet<>();
        //for (int i = 10; i <= 17; i++) {
        //    binaryTreeDepthList.add(i);
        //}
        //binaryTreeDepthList.add(16);
        binaryTreeDepthList.add(17);

        initialEchoAxioms = new UnifiedSet<>();
        //initialEchoAxioms.add(10_000);
        //initialEchoAxioms.add(20_000);
        //initialEchoAxioms.add(50_000);
        //initialEchoAxioms.add(100_000);
        //initialEchoAxioms.add(200_000);
        //initialEchoAxioms.add(500_000);
        initialEchoAxioms.add(2_000_000);
        //initialEchoAxioms.add(10_000_000);

        chainDepthList = new UnifiedSet<>();
        for (int i = 100; i <= 1000; i += 100) {
            chainDepthList.add(i);
        }

        numberOfWorkersList = new UnifiedSet<>();
        //numberOfWorkersList.add(1);
        //numberOfWorkersList.add(2);
        //numberOfWorkersList.add(3);
        numberOfWorkersList.add(4);

        messageDistributionTypes = new UnifiedSet<>();
        //messageDistributionTypes.add(MessageDistributionType.SEND_ALL_MESSAGES_OVER_NETWORK);
        messageDistributionTypes.add(MessageDistributionType.ADD_OWN_MESSAGES_DIRECTLY_TO_TODO);
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
        //includedApproaches.add(SaturationApproach.SINGLE_THREADED);
        //includedApproaches.add(SaturationApproach.MULTITHREADED);
        includedApproaches.add(SaturationApproach.DISTRIBUTED_MULTITHREADED);
        //includedApproaches.add(SaturationApproach.DISTRIBUTED_SEPARATE_JVM);
        //includedApproaches.add(SaturationApproach.DISTRIBUTED_SEPARATE_DOCKER_CONTAINER);

        //binaryTreeBenchmark(outputDirectory, WARM_UP_ROUNDS, EXPERIMENT_ROUNDS,
        //        includedApproaches, binaryTreeDepthList, numberOfWorkersList, collectWorkerNodeStatistics, messageDistributionTypes);
        //chainGraphBenchmark(outputDirectory, WARM_UP_ROUNDS, EXPERIMENT_ROUNDS,
        //        includedApproaches, chainDepthList, numberOfWorkersList, collectWorkerNodeStatistics, messageDistributionTypes);
        //echoBenchmark(outputDirectory, WARM_UP_ROUNDS, EXPERIMENT_ROUNDS,
        //        includedApproaches, initialEchoAxioms, numberOfWorkersList, collectWorkerNodeStatistics, messageDistributionTypes);
        randomDigraphBenchmark(outputDirectory, WARM_UP_ROUNDS, EXPERIMENT_ROUNDS,
                includedApproaches, randomDigraphNodes, numberOfWorkersList, collectWorkerNodeStatistics, messageDistributionTypes);
    }

    public static void binaryTreeBenchmark(File outputDirectory,
                                           int warmUpRounds,
                                           int experimentRounds,
                                           Set<SaturationApproach> includedApproaches,
                                           Set<Integer> binaryTreeDepthList,
                                           Set<Integer> numberOfWorkersList,
                                           boolean collectWorkerNodeStatistics,
                                           Set<MessageDistributionType> messageDistributionTypes) {

        SaturationBenchmark<ReachabilityClosure, Reachability> binaryTreeBenchmark = new SaturationBenchmark<>(
                "BinaryTree",
                includedApproaches,
                outputDirectory,
                warmUpRounds,
                experimentRounds,
                collectWorkerNodeStatistics,
                messageDistributionTypes
        );
        for (Integer depth : binaryTreeDepthList) {
            ReachabilityBinaryTreeGenerator treeGenerator = new ReachabilityBinaryTreeGenerator(depth);
            for (Integer numWorkers : numberOfWorkersList) {
                ReachabilitySaturationInitializationFactory factory = new ReachabilitySaturationInitializationFactory(
                        treeGenerator,
                        numWorkers,
                        0
                );
                binaryTreeBenchmark.startBenchmark(factory);
            }
        }
        binaryTreeBenchmark.finishBenchmark();
    }

    public static void randomDigraphBenchmark(File outputDirectory,
                                           int warmUpRounds,
                                           int experimentRounds,
                                           Set<SaturationApproach> includedApproaches,
                                           Set<Integer> randomDigraphNodes,
                                           Set<Integer> numberOfWorkersList,
                                           boolean collectWorkerNodeStatistics,
                                           Set<MessageDistributionType> messageDistributionTypes) {

        SaturationBenchmark<ReachabilityClosure, Reachability> randomDigraphBenchmark = new SaturationBenchmark<>(
                "RandomDigraph",
                includedApproaches,
                outputDirectory,
                warmUpRounds,
                experimentRounds,
                collectWorkerNodeStatistics,
                messageDistributionTypes
        );
        for (Integer numberOfNodes : randomDigraphNodes) {
            ReachabilityRandomDigraphGenerator graphGenerator = new ReachabilityRandomDigraphGenerator(numberOfNodes, 0.2);
            for (Integer numWorkers : numberOfWorkersList) {
                ReachabilitySaturationInitializationFactory factory = new ReachabilitySaturationInitializationFactory(
                        graphGenerator,
                        numWorkers,
                        0
                );
                randomDigraphBenchmark.startBenchmark(factory);
            }
        }
        randomDigraphBenchmark.finishBenchmark();
    }

    public static void chainGraphBenchmark(File outputDirectory,
                                           int warmUpRounds,
                                           int experimentRounds,
                                           Set<SaturationApproach> includedApproaches,
                                           Set<Integer> chainDepthList,
                                           Set<Integer> numberOfWorkersList,
                                           boolean collectWorkerNodeStatistics,
                                           Set<MessageDistributionType> messageDistributionTypes) {

        SaturationBenchmark<ReachabilityClosure, Reachability> chainGraphBenchmark = new SaturationBenchmark<>(
                "ChainGraph",
                includedApproaches,
                outputDirectory,
                warmUpRounds,
                experimentRounds,
                collectWorkerNodeStatistics,
                messageDistributionTypes
        );
        for (Integer depth : chainDepthList) {
            ReachabilityChainGraphGenerator graphGenerator = new ReachabilityChainGraphGenerator(depth);
            for (Integer numWorkers : numberOfWorkersList) {
                ReachabilitySaturationInitializationFactory factory = new ReachabilitySaturationInitializationFactory(
                        graphGenerator,
                        numWorkers,
                        0
                );
                chainGraphBenchmark.startBenchmark(factory);
            }
        }
        chainGraphBenchmark.finishBenchmark();
    }

    public static void echoBenchmark(File outputDirectory,
                                     int warmUpRounds,
                                     int experimentRounds,
                                     Set<SaturationApproach> includedApproaches,
                                     Set<Integer> initialMessagesList,
                                     Set<Integer> numberOfWorkersList,
                                     boolean collectWorkerNodeStatistics,
                                     Set<MessageDistributionType> messageDistributionTypes) {
        SaturationBenchmark<EchoClosure, EchoAxiom> echoBenchmark = new SaturationBenchmark<>(
                "Echo",
                includedApproaches,
                outputDirectory,
                warmUpRounds,
                experimentRounds,
                collectWorkerNodeStatistics,
                messageDistributionTypes
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
