package transitiveclosure;

import benchmark.graphgeneration.ReachabilityBinaryTreeGenerator;
import benchmark.transitiveclosure.*;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.ClosureComputationTestUtil;

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
        ReachabilitySaturationInitializationFactory initializationFactory = new ReachabilitySaturationInitializationFactory(
                initialAxioms,
                1,
                0
        );

        assertEquals(expectedResults, ClosureComputationTestUtil.singleThreadedClosureComputation(initializationFactory));
    }


    @Test
    void testDistributedClosureComputation() {
        boolean workersInSeparateJVM = false;
        int numberOfThreadsPerSingleWorker = 1;
        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                workersInSeparateJVM = true;
            }
            ReachabilitySaturationInitializationFactory initializationFactory = new ReachabilitySaturationInitializationFactory(
                    initialAxioms,
                    2,
                    0
            );
            ClosureComputationTestUtil.distributedClosureComputation(initializationFactory, workersInSeparateJVM, numberOfThreadsPerSingleWorker);

            ReachabilityBinaryTreeGenerator generator = new ReachabilityBinaryTreeGenerator(5);
            initializationFactory = new ReachabilitySaturationInitializationFactory(
                    generator.generateGraph(),
                    4,
                    0
            );
            ClosureComputationTestUtil.distributedClosureComputation(initializationFactory, workersInSeparateJVM, numberOfThreadsPerSingleWorker);

            if (!workersInSeparateJVM) {
                generator = new ReachabilityBinaryTreeGenerator(7);
                initializationFactory = new ReachabilitySaturationInitializationFactory(
                        generator.generateGraph(),
                        20,
                        0
                );
                ClosureComputationTestUtil.distributedClosureComputation(initializationFactory, workersInSeparateJVM, numberOfThreadsPerSingleWorker);
            }
        }
    }

    @Test
    void testParallelClosureComputation() {
        ReachabilitySaturationInitializationFactory initializationFactory = new ReachabilitySaturationInitializationFactory(
                initialAxioms,
                4,
                0
        );
        ClosureComputationTestUtil.parallelClosureComputation(initializationFactory);

        ReachabilityBinaryTreeGenerator generator = new ReachabilityBinaryTreeGenerator(5);
        initializationFactory = new ReachabilitySaturationInitializationFactory(
                generator.generateGraph(),
                4,
                0
        );
        ClosureComputationTestUtil.parallelClosureComputation(initializationFactory);

        generator = new ReachabilityBinaryTreeGenerator(8);
        initializationFactory = new ReachabilitySaturationInitializationFactory(
                generator.generateGraph(),
                20,
                0
        );
        ClosureComputationTestUtil.parallelClosureComputation(initializationFactory);

        generator = new ReachabilityBinaryTreeGenerator(10);
        initializationFactory = new ReachabilitySaturationInitializationFactory(
                generator.generateGraph(),
                20,
                0
        );
        ClosureComputationTestUtil.parallelClosureComputation(initializationFactory);

        generator = new ReachabilityBinaryTreeGenerator(12);
        initializationFactory = new ReachabilitySaturationInitializationFactory(
                generator.generateGraph(),
                20,
                0
        );
        ClosureComputationTestUtil.parallelClosureComputation(initializationFactory);

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
