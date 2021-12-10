package benchmark.transitiveclosure.experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class IndividualExperiments {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Press ENTER to start benchmark...");
        scanner.nextLine();
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
        //binaryTreeDepth.add(5);
        //binaryTreeDepth.add(8);
        //binaryTreeDepth.add(10);
        binaryTreeDepth.add(10);
        //binaryTreeDepth.add(12);

        //binaryTreeDepth.add(5);
        //binaryTreeDepth.add(10);
        //binaryTreeDepth.add(15);
        //binaryTreeDepth.add(20);

        List<Integer> numberOfWorkers = new ArrayList<>();
        //numberOfWorkers.add(1);
        //numberOfWorkers.add(2);
        numberOfWorkers.add(4);

        // BWUniCluster has maximum of 40 threads - 2 threads per worker, thus, 20 threads at max.
        //numberOfWorkers.add(1);
        //numberOfWorkers.add(4);
        //numberOfWorkers.add(8);
        //numberOfWorkers.add(10);
        // numberOfWorkers.add(15);
        //numberOfWorkers.add(20);

        List<Integer> maxNumberOfAxiomsToBuffer = new ArrayList<>();
        maxNumberOfAxiomsToBuffer.add(1);
        maxNumberOfAxiomsToBuffer.add(10);
        maxNumberOfAxiomsToBuffer.add(20);
        maxNumberOfAxiomsToBuffer.add(100);

        ReachabilityBenchmark benchmark = new ReachabilityBenchmark(outputDirectory, 3);
        benchmark.startBinaryTreeBenchmark(
                binaryTreeDepth,
                numberOfWorkers,
                maxNumberOfAxiomsToBuffer
        );
    }
}
