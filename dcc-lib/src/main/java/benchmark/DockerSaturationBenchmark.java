package benchmark;

import enums.MessageDistributionType;
import enums.SaturationApproach;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import util.ConsoleUtils;

import java.io.File;
import java.util.Set;
import java.util.logging.Logger;

public class DockerSaturationBenchmark {

    private static final boolean collectWorkerNodeStatistics = false;
    private static final int EXPERIMENT_ROUNDS = 3;
    private static final int WARM_UP_ROUNDS = 2;

    private static final File outputDirectory = new File("saturation");

    private static Set<Integer> binaryTreeDepthList;
    private static Set<Integer> initialEchoAxioms;
    private static Set<Integer> chainDepthList;
    private static Set<Integer> numberOfWorkersList;
    private static Set<MessageDistributionType> messageDistributionTypes;

    static {
        messageDistributionTypes = new UnifiedSet<>();
        messageDistributionTypes.add(MessageDistributionType.SEND_ALL_MESSAGES_OVER_NETWORK);
        messageDistributionTypes.add(MessageDistributionType.ADD_OWN_MESSAGES_DIRECTLY_TO_TODO);
    }

    static {
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
        // args: <APPROACH> <NUM_WORKERS> <BENCHMARK>
        String approach = args[0];
        int numWorkers = Integer.parseInt(args[1]);
        String benchmark = args[2];

        numberOfWorkersList = new UnifiedSet<>();
        numberOfWorkersList.add(numWorkers);

        Set<SaturationApproach> includedApproaches = new UnifiedSet<>();
        switch (approach) {
            case "single-machine":
                includedApproaches.add(SaturationApproach.SINGLE_THREADED);
                includedApproaches.add(SaturationApproach.PARALLEL);
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
                IndividualExperiments.echoBenchmark(outputDirectory, WARM_UP_ROUNDS, EXPERIMENT_ROUNDS, includedApproaches,
                        initialEchoAxioms, numberOfWorkersList, collectWorkerNodeStatistics, messageDistributionTypes);
                break;
            case "binarytree":
                IndividualExperiments.binaryTreeBenchmark(outputDirectory, WARM_UP_ROUNDS, EXPERIMENT_ROUNDS, includedApproaches,
                        binaryTreeDepthList, numberOfWorkersList, collectWorkerNodeStatistics, messageDistributionTypes);
                break;
            case "chaingraph":
                IndividualExperiments.chainGraphBenchmark(outputDirectory, WARM_UP_ROUNDS, EXPERIMENT_ROUNDS, includedApproaches,
                        chainDepthList, numberOfWorkersList, collectWorkerNodeStatistics, messageDistributionTypes);
                break;
            default:
                throw new IllegalArgumentException("Unknown benchmark type: " + benchmark + ", allowed: echo | binarytree | chaingraph");
        }
    }

}

