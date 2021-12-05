package elsyntax;

import benchmark.*;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reasoning.saturation.SingleThreadedSaturation;

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
        SingleThreadedSaturation<ReachabilityClosure, Reachability> saturation = new SingleThreadedSaturation<>(
                initialAxioms.iterator(),
                ReachabilityWorkerFactory.getReachabilityRules(),
                new ReachabilityClosure()
        );

        ReachabilityClosure closure = saturation.saturate();
        Set<Reachability> result = new UnifiedSet<>();
        closure.getClosureResults().forEach(result::add);

        System.out.println("Closure: ");
        closure.getClosureResults().forEach(System.out::println);

        assertEquals(expectedResults, result);
    }
}
