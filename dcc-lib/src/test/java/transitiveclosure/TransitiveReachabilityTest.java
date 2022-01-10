package transitiveclosure;

import benchmark.workergeneration.SaturationJVMWorkerGenerator;
import benchmark.workergeneration.SaturationWorkerGenerator;
import benchmark.workergeneration.SaturationWorkerThreadGenerator;
import benchmark.graphgeneration.ReachabilityBinaryTreeGenerator;
import benchmark.transitiveclosure.*;
import networking.ServerData;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.RoaringBitmap;
import reasoning.saturation.SingleThreadedSaturation;
import reasoning.saturation.distributed.DistributedSaturation;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.parallel.ParallelSaturation;
import util.ConsoleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransitiveReachabilityTest {

    private List<Reachability> initialAxioms;
    private Set<Reachability> expectedResults;

    @BeforeEach
    public void init() {
        /*
        told axioms:
            1 - 2
            2 - 3
            3 - 4
        expected derived:
            1 - 3
            1 - 4
            2 - 4
         */
        expectedResults = new UnifiedSet<>();
        expectedResults.add(new ToldReachability(1, 2));
        expectedResults.add(new ToldReachability(2, 3));
        expectedResults.add(new ToldReachability(3, 4));

        expectedResults.add(new DerivedReachability(1, 2));
        expectedResults.add(new DerivedReachability(2, 3));
        expectedResults.add(new DerivedReachability(3, 4));

        expectedResults.add(new DerivedReachability(1, 3));
        expectedResults.add(new DerivedReachability(1, 4));
        expectedResults.add(new DerivedReachability(2, 4));

        initialAxioms = new ArrayList<>();
        initialAxioms.add(new ToldReachability(1, 2));
        initialAxioms.add(new ToldReachability(2, 3));
        initialAxioms.add(new ToldReachability(3, 4));
    }


    @Test
    void testSingleThreadedComputation() {
        assertEquals(expectedResults, singleThreadedClosureComputation(initialAxioms));
    }

    Set<Reachability> singleThreadedClosureComputation(List<? extends Reachability> initialAxioms) {
        ReachabilitySaturationInitializationFactory initializationFactory = new ReachabilitySaturationInitializationFactory(
                initialAxioms,
                1,
                0
        );

        SingleThreadedSaturation<ReachabilityClosure, Reachability> saturation = new SingleThreadedSaturation<>(
                initialAxioms.iterator(),
                initializationFactory.generateRules(),
                new ReachabilityClosure()
        );

        ReachabilityClosure closure = saturation.saturate();
        Set<Reachability> result = new UnifiedSet<>();
        closure.getClosureResults().forEach(result::add);
        System.out.println("Closure: ");
        result.forEach(System.out::println);
        return result;
    }

    Set<Reachability> parallelClosureComputation(List<? extends Reachability> initialAxioms, int numberOfWorkers) {
        ParallelSaturation<ReachabilityClosure, Reachability, RoaringBitmap> saturation = new ParallelSaturation<>(
                new ReachabilitySaturationInitializationFactory(initialAxioms, numberOfWorkers, 0)
        );

        ReachabilityClosure closure = saturation.saturate();
        Set<Reachability> result = new UnifiedSet<>();
        closure.getClosureResults().forEach(result::add);

        assertEquals(singleThreadedClosureComputation(initialAxioms), result);

        return result;
    }


    void distributedClosureComputation(List<? extends Reachability> initialAxioms, int numberOfWorkers,
                                       boolean workersInSeparateJVMs) {
        List<ServerData> serverDataList = null;
        SaturationWorkerGenerator workerGenerator;

        if (workersInSeparateJVMs) {
            workerGenerator = new SaturationJVMWorkerGenerator(numberOfWorkers);
            try {
                workerGenerator.generateAndRunWorkers();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } else {
            workerGenerator = new SaturationWorkerThreadGenerator(
                    numberOfWorkers);
            workerGenerator.generateAndRunWorkers();
        }
        serverDataList = workerGenerator.getWorkerServerDataList();


        ReachabilitySaturationInitializationFactory initializationFactory = new ReachabilitySaturationInitializationFactory(
                initialAxioms,
                numberOfWorkers,
                0
        );

        List<DistributedWorkerModel<ReachabilityClosure, Reachability, RoaringBitmap>> workers = initializationFactory.getDistributedWorkerModels(
                serverDataList);
        SaturationConfiguration configuration = new SaturationConfiguration(true, false);
        DistributedSaturation<ReachabilityClosure, Reachability, RoaringBitmap> saturation = new DistributedSaturation<>(
                workers,
                new ReachabilityWorkloadDistributor(workers),
                initialAxioms,
                new ReachabilityClosure(),
                configuration
        );

        ReachabilityClosure closure = saturation.saturate();
        Set<Reachability> distributedResults = new UnifiedSet<>();
        distributedResults.addAll(closure.getClosureResults());

        System.out.println("Closure: ");
        closure.getClosureResults().forEach(System.out::println);

        Set<Reachability> singleThreadedResults = singleThreadedClosureComputation(initialAxioms);
        assertEquals(singleThreadedResults, distributedResults);

        List<WorkerStatistics> workerStatistics = saturation.getWorkerStatistics();
        ControlNodeStatistics controlNodeStatistics = saturation.getControlNodeStatistics();

        System.out.println(ConsoleUtils.getSeparator());
        System.out.println("Statistics");
        System.out.println(ConsoleUtils.getSeparator());
        System.out.println(WorkerStatistics.getWorkerStatsHeader());
        workerStatistics.forEach(w -> System.out.println(w.getWorkerStatistics()));

        System.out.println(ControlNodeStatistics.getControlNodeStatsHeader());
        System.out.println(controlNodeStatistics.getControlNodeStatistics());
        System.out.println(ConsoleUtils.getSeparator());

        workerGenerator.stopWorkers();

        if (workersInSeparateJVMs) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    void testDistributedClosureComputation() {
        boolean workersInSeparateJVM = false;
        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                workersInSeparateJVM = true;
            }
            distributedClosureComputation(initialAxioms, 2, workersInSeparateJVM);

            ReachabilityBinaryTreeGenerator generator = new ReachabilityBinaryTreeGenerator(5);
            distributedClosureComputation(generator.generateGraph(), 4, workersInSeparateJVM);

            if (!workersInSeparateJVM) {
                generator = new ReachabilityBinaryTreeGenerator(7);
                distributedClosureComputation(generator.generateGraph(), 20, workersInSeparateJVM);
            }
        }
    }

    @Test
    void testParallelClosureComputation() {
        parallelClosureComputation(initialAxioms, 4);

        ReachabilityBinaryTreeGenerator generator = new ReachabilityBinaryTreeGenerator(5);
        parallelClosureComputation(generator.generateGraph(), 4);

        generator = new ReachabilityBinaryTreeGenerator(8);
        parallelClosureComputation(generator.generateGraph(), 20);

        generator = new ReachabilityBinaryTreeGenerator(10);
        parallelClosureComputation(generator.generateGraph(), 20);

        generator = new ReachabilityBinaryTreeGenerator(12);
        parallelClosureComputation(generator.generateGraph(), 20);

    }

    @Test
    void testBinaryTreeGeneration() {
        ReachabilityBinaryTreeGenerator generator = new ReachabilityBinaryTreeGenerator(3);
        System.out.println("Depth: " + 3);
        List<ToldReachability> edges = generator.generateGraph();
        edges.forEach(System.out::println);

        System.out.println();
        System.out.println("Depth: " + 4);
        generator = new ReachabilityBinaryTreeGenerator(4);
        edges = generator.generateGraph();
        edges.forEach(System.out::println);
        assertEquals(14, generator.getNumberOfEdgesOriginalGraph());
        assertEquals(14, edges.size());
        assertEquals(20, generator.getNumberOfEdgesInTransitiveClosure());
        assertEquals(34, generator.getTotalNumberOfEdges());
        assertEquals(4, generator.getDiameter());
    }
}
