package benchmark;

import enums.MessageDistributionType;
import enums.SaturationApproach;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.io.File;
import java.util.Collections;
import java.util.Set;

/**
 * This class represents a console application which can be used to execute a closure computation benchmarks for different problems.
 */
public class SaturationBenchmarkCLApp {

    private static final File outputDirectory = new File("saturation");
    private static final Set<Integer> chainDepthList;
    private static final Set<MessageDistributionType> messageDistributionTypes;
    private static boolean collectWorkerNodeStatistics = false;
    private static int EXPERIMENT_ROUNDS = 3;
    private static int WARM_UP_ROUNDS = 2;
    private static Set<Integer> randomDigraphNodes;
    private static Set<Integer> binaryTreeDepthList;
    private static Set<Integer> initialEchoAxioms;
    private static Set<Integer> numberOfWorkersList;

    static {
        messageDistributionTypes = new UnifiedSet<>();
        messageDistributionTypes.add(MessageDistributionType.SEND_ALL_MESSAGES_OVER_NETWORK);
        messageDistributionTypes.add(MessageDistributionType.ADD_OWN_MESSAGES_DIRECTLY_TO_TODO);
    }

    static {
        randomDigraphNodes = new UnifiedSet<>();
        //randomDigraphNodes.add(480);
        randomDigraphNodes.add(512);
        //randomDigraphNodes.add(960);

        binaryTreeDepthList = new UnifiedSet<>();
        for (int i = 17; i <= 20; i++) {
            binaryTreeDepthList.add(i);
        }

        initialEchoAxioms = new UnifiedSet<>();
        initialEchoAxioms.add(500_000);
        initialEchoAxioms.add(1_000_000);
        initialEchoAxioms.add(5_000_000);
        initialEchoAxioms.add(10_000_000);

        chainDepthList = new UnifiedSet<>();
        for (int i = 100; i <= 1000; i += 100) {
            chainDepthList.add(i);
        }
    }

    public static void main(String[] args) {
        // args: <APPROACH> <NUM_WORKERS> <BENCHMARK> <COLLECT-WORKER-STATS>
        String approach = args[0];
        int numWorkers = Integer.parseInt(args[1]);
        String benchmark = args[2];

        // collect worker statistics
        if (args.length > 3) {
            collectWorkerNodeStatistics = Boolean.parseBoolean(args[3]);
            if (collectWorkerNodeStatistics) {
                WARM_UP_ROUNDS = 1;
                EXPERIMENT_ROUNDS = 1;
                initialEchoAxioms = Collections.singleton(5_000_000);
                binaryTreeDepthList = Collections.singleton(19);
                randomDigraphNodes = Collections.singleton(480);
            }
        } else {
            collectWorkerNodeStatistics = false;
        }

        numberOfWorkersList = new UnifiedSet<>();
        numberOfWorkersList.add(numWorkers);

        Set<SaturationApproach> includedApproaches = new UnifiedSet<>();
        switch (approach) {
            case "single-machine":
                includedApproaches.add(SaturationApproach.SINGLE_THREADED);
                includedApproaches.add(SaturationApproach.MULTITHREADED);
                includedApproaches.add(SaturationApproach.DISTRIBUTED_MULTITHREADED);
                includedApproaches.add(SaturationApproach.DISTRIBUTED_SEPARATE_JVM);
                break;
            case "docker-network":
                includedApproaches.add(SaturationApproach.DISTRIBUTED_DOCKER);
                break;
            default:
                throw new IllegalArgumentException("allowed: single-machine | docker-network");
        }

        switch (benchmark.toLowerCase()) {
            case "echo":
                IndividualSaturationBenchmarks.echoBenchmark(outputDirectory, WARM_UP_ROUNDS, EXPERIMENT_ROUNDS, includedApproaches,
                        initialEchoAxioms, numberOfWorkersList, collectWorkerNodeStatistics, messageDistributionTypes);
                break;
            case "binarytree":
                IndividualSaturationBenchmarks.binaryTreeBenchmark(outputDirectory, WARM_UP_ROUNDS, EXPERIMENT_ROUNDS, includedApproaches,
                        binaryTreeDepthList, numberOfWorkersList, collectWorkerNodeStatistics, messageDistributionTypes);
                break;
            case "chaingraph":
                IndividualSaturationBenchmarks.chainGraphBenchmark(outputDirectory, WARM_UP_ROUNDS, EXPERIMENT_ROUNDS, includedApproaches,
                        chainDepthList, numberOfWorkersList, collectWorkerNodeStatistics, messageDistributionTypes);
                break;
            case "randomdigraph":
                IndividualSaturationBenchmarks.randomDigraphBenchmark(outputDirectory, WARM_UP_ROUNDS, EXPERIMENT_ROUNDS,
                        includedApproaches,
                        randomDigraphNodes, numberOfWorkersList, collectWorkerNodeStatistics, messageDistributionTypes);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown benchmark type: " + benchmark + ", allowed: echo | binarytree | chaingraph | randomdigraph");
        }
    }

}

