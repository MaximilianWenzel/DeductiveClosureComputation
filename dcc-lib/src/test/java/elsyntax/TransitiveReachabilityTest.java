package elsyntax;

import benchmark.*;
import benchmark.graphgeneration.ReachabilityBinaryTreeGenerator;
import networking.ServerData;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.RoaringBitmap;
import reasoning.saturation.SingleThreadedSaturation;
import reasoning.saturation.distributed.DistributedSaturation;
import reasoning.saturation.distributed.SaturationWorker;
import reasoning.saturation.distributed.communication.BenchmarkConfiguration;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.parallel.ParallelSaturation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

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
        SingleThreadedSaturation<ReachabilityClosure, Reachability> saturation = new SingleThreadedSaturation<>(
                initialAxioms.iterator(),
                ReachabilityWorkerFactory.getReachabilityRules(),
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
                new ReachabilitySaturationInitializationFactory(initialAxioms, numberOfWorkers)
        );

        ReachabilityClosure closure = saturation.saturate();
        Set<Reachability> result = new UnifiedSet<>();
        closure.getClosureResults().forEach(result::add);

        assertEquals(singleThreadedClosureComputation(initialAxioms), result);

        return result;
    }


    void distributedClosureComputation(List<? extends Reachability> initialAxioms, int numberOfWorkers) {
        BenchmarkConfiguration benchmarkConfiguration = new BenchmarkConfiguration(10);


        SaturationWorkerServerGenerator<ReachabilityClosure, Reachability, RoaringBitmap> workerServerFactory;
        workerServerFactory = new SaturationWorkerServerGenerator<>(benchmarkConfiguration, numberOfWorkers, new Callable<>() {
            @Override
            public ReachabilityClosure call() throws Exception {
                return new ReachabilityClosure();
            }
        });

        List<SaturationWorker<ReachabilityClosure, Reachability, RoaringBitmap>> saturationWorkers;
        saturationWorkers = workerServerFactory.generateWorkers();
        List<Thread> threads = saturationWorkers.stream().map(Thread::new).collect(Collectors.toList());
        threads.forEach(Thread::start);

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

        ReachabilityClosure closure = saturation.saturate();
        Set<Reachability> distributedResults = new UnifiedSet<>();
        distributedResults.addAll(closure.getClosureResults());

        System.out.println("Closure: ");
        closure.getClosureResults().forEach(System.out::println);

        Set<Reachability> singleThreadedResults = singleThreadedClosureComputation(initialAxioms);
        assertEquals(singleThreadedResults, distributedResults);

        saturationWorkers.forEach(SaturationWorker::terminate);
    }

    @Test
    void testDistributedClosureComputation() {
        distributedClosureComputation(initialAxioms, 4);

        ReachabilityBinaryTreeGenerator generator = new ReachabilityBinaryTreeGenerator(5);
        distributedClosureComputation(generator.generateGraph(), 4);

        generator = new ReachabilityBinaryTreeGenerator(8);
        distributedClosureComputation(generator.generateGraph(), 20);
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
