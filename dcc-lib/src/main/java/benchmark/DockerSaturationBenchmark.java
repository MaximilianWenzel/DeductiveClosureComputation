package benchmark;

import enums.SaturationApproach;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import util.ConsoleUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class DockerSaturationBenchmark {

    private static final Logger log = ConsoleUtils.getLogger();
    private static final File outputDirectory = new File("saturation");

    private static List<Integer> binaryTreeDepthList;
    private static List<Integer> initialEchoAxioms;
    private static List<Integer> chainDepthList;
    private static List<Integer> numberOfWorkersList;

    static {
        binaryTreeDepthList = new ArrayList<>();
        binaryTreeDepthList.add(10);

        initialEchoAxioms = new ArrayList<>();
        //initialEchoAxioms.add(10_000);
        initialEchoAxioms.add(20_000);
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
    }

    public static void main(String[] args) {
        // args: <APPROACH> <NUM_WORKERS> <BENCHMARK>
        String approach = args[0];
        int numWorkers = Integer.parseInt(args[1]);
        String benchmark = args[2];

        numberOfWorkersList = new ArrayList<>();
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
                includedApproaches.add(SaturationApproach.DISTRIBUTED_DOCKER_BENCHMARK);
                break;
            default:
                throw new IllegalArgumentException("allowed: single-machine | docker-network");
        }

        switch (benchmark.toLowerCase()) {
            case "echo":
                IndividualExperiments.echoBenchmark(outputDirectory, includedApproaches, initialEchoAxioms, numberOfWorkersList);
                break;
            case "binarytree":
                IndividualExperiments.binaryTreeBenchmark(outputDirectory, includedApproaches, binaryTreeDepthList, numberOfWorkersList);
                break;
            case "chaingraph":
                IndividualExperiments.chainGraphBenchmark(outputDirectory, includedApproaches, chainDepthList, numberOfWorkersList);
                break;
            default:
                throw new IllegalArgumentException("Unknown benchmark type: " + benchmark + ", allowed: echo | binarytree | chaingraph");
        }
    }

}
