package transitiveclosure;

import benchmark.echoclosure.EchoAxiom;
import benchmark.echoclosure.EchoClosure;
import benchmark.echoclosure.EchoSaturationInitializationFactory;
import org.junit.jupiter.api.Test;
import reasoning.saturation.SaturationInitializationFactory;
import util.ClosureComputationTestUtil;

public class EchoSaturationTest {


    @Test
    void testSingleThreadedEchoSaturation() {
        SaturationInitializationFactory<EchoClosure, EchoAxiom> initializationFactory = new EchoSaturationInitializationFactory(
                1, 5);

        ClosureComputationTestUtil.singleThreadedClosureComputation(initializationFactory);
    }

    @Test
    void testParallelEchoSaturation() {
        SaturationInitializationFactory<EchoClosure, EchoAxiom> initializationFactory = new EchoSaturationInitializationFactory(
                4, 100);

        ClosureComputationTestUtil.parallelClosureComputation(initializationFactory);
    }

    @Test
    void testDistributedEchoSaturation() {
        int numWorkers = 2;
        SaturationInitializationFactory<EchoClosure, EchoAxiom> initializationFactory = new EchoSaturationInitializationFactory(
                numWorkers, 1_000);

        ClosureComputationTestUtil.distributedClosureComputation(initializationFactory, false, 1, false);

        numWorkers = 1;
        initializationFactory = new EchoSaturationInitializationFactory(
                numWorkers, 1_000);
        ClosureComputationTestUtil.distributedClosureComputation(initializationFactory, false, 1, false);
    }
}
