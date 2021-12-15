package benchmark.transitiveclosure.experiments;

import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IndividualExperiments {

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
        List<Integer> binaryTreeDepth = new ArrayList<>();

        for (int i = 10; i < 13; i++) {
            binaryTreeDepth.add(i);
        }

        //binaryTreeDepth.add(12);
        //binaryTreeDepth.add(13);
        //binaryTreeDepth.add(14);
        //binaryTreeDepth.add(15);
        //binaryTreeDepth.add(16);

        List<Integer> chainDepth = new ArrayList<>();
        //chainDepth.add(200);
        //chainDepth.add(400);
        //chainDepth.add(600);

        //binaryTreeDepth.add(5);
        //binaryTreeDepth.add(10);
        //binaryTreeDepth.add(15);
        //binaryTreeDepth.add(20);

        List<Integer> numberOfWorkers = new ArrayList<>();
        //numberOfWorkers.add(1);
        numberOfWorkers.add(2);
        numberOfWorkers.add(4);
        //numberOfWorkers.add(8);

        List<Integer> maxNumberOfAxiomsToBuffer = new ArrayList<>();
        //maxNumberOfAxiomsToBuffer.add(1);
        //maxNumberOfAxiomsToBuffer.add(10);
        maxNumberOfAxiomsToBuffer.add(50);
        //maxNumberOfAxiomsToBuffer.add(100);

        List<Integer> ruleDelaysInNanoSec = new ArrayList<>();
        ruleDelaysInNanoSec.add(0); // 0
        ruleDelaysInNanoSec.add(1_000); // 0.001ms
        ruleDelaysInNanoSec.add(10_000); // 0.01ms
        ruleDelaysInNanoSec.add(50_000);
        ruleDelaysInNanoSec.add(80_000);
        ruleDelaysInNanoSec.add(100_000); // 0.1ms
        ruleDelaysInNanoSec.add(200_000);
        ruleDelaysInNanoSec.add(500_000);
        //ruleDelaysInNanoSec.add(1_000_000); // 1ms
        //ruleDelaysInNanoSec.add(10_000_000); // 10ms
        //ruleDelaysInNanoSec.add(100_000_000); // 100ms
        //ruleDelaysInNanoSec.add(1_000_000_000); // 1s

        Set<ReachabilityBenchmark.Approach> includedApproaches = new UnifiedSet<>();
        includedApproaches.add(ReachabilityBenchmark.Approach.SINGLE_THREADED);
        includedApproaches.add(ReachabilityBenchmark.Approach.PARALLEL);
        includedApproaches.add(ReachabilityBenchmark.Approach.DISTRIBUTED);

        ReachabilityBenchmark benchmark = new ReachabilityBenchmark(outputDirectory, 3, includedApproaches);
        benchmark.startBinaryTreeBenchmark(
                binaryTreeDepth,
                numberOfWorkers,
                maxNumberOfAxiomsToBuffer,
                ruleDelaysInNanoSec
        );

        /*
        benchmark.startChainGraphBenchmark(
                chainDepth,
                numberOfWorkers,
                maxNumberOfAxiomsToBuffer
        );

         */
    }
}
