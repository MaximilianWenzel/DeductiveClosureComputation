package transitiveclosure;

import benchmark.echoclosure.EchoAxiom;
import benchmark.echoclosure.EchoClosure;
import benchmark.echoclosure.EchoSaturationInitializationFactory;
import org.junit.jupiter.api.Test;
import util.ClosureComputationTestUtil;
import reasoning.saturation.SaturationInitializationFactory;

public class EchoSaturationTest {


    @Test
    void testSingleThreadedEchoSaturation() {
        SaturationInitializationFactory<EchoClosure, EchoAxiom, Integer> initializationFactory = new EchoSaturationInitializationFactory(
                1, 5);

        ClosureComputationTestUtil.singleThreadedClosureComputation(initializationFactory);
    }

    @Test
    void testParallelEchoSaturation() {
        SaturationInitializationFactory<EchoClosure, EchoAxiom, Integer> initializationFactory = new EchoSaturationInitializationFactory(
                4, 100);

        ClosureComputationTestUtil.parallelClosureComputation(initializationFactory);
    }

    @Test
    void testDistributedEchoSaturation() {
        int numWorkers = 4;
        SaturationInitializationFactory<EchoClosure, EchoAxiom, Integer> initializationFactory = new EchoSaturationInitializationFactory(
                numWorkers, 1000);

        ClosureComputationTestUtil.distributedClosureComputation(initializationFactory, false);
    }
}
