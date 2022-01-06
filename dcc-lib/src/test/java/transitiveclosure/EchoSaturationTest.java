package transitiveclosure;

import benchmark.echoclosure.EchoAxiom;
import benchmark.echoclosure.EchoClosure;
import benchmark.echoclosure.EchoSaturationInitializationFactory;
import benchmark.workergeneration.SaturationWorkerThreadGenerator;
import networking.ServerData;
import org.junit.jupiter.api.Test;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.SingleThreadedSaturation;
import reasoning.saturation.distributed.DistributedSaturation;
import reasoning.saturation.parallel.ParallelSaturation;

import java.util.List;

public class EchoSaturationTest {


    @Test
    void testSingleThreadedEchoSaturation() {
        SaturationInitializationFactory<EchoClosure, EchoAxiom, Integer> initializationFactory = new EchoSaturationInitializationFactory(
                1, 5);

        SingleThreadedSaturation<EchoClosure, EchoAxiom> singleThreadedSaturation = new SingleThreadedSaturation<>(
                initializationFactory.getInitialAxioms().iterator(),
                initializationFactory.generateRules(),
                initializationFactory.getNewClosure()
        );

        EchoClosure closure = singleThreadedSaturation.saturate();

        System.out.println(closure.getClosureResults());
    }

    @Test
    void testParallelEchoSaturation() {
        SaturationInitializationFactory<EchoClosure, EchoAxiom, Integer> initializationFactory = new EchoSaturationInitializationFactory(
                4, 100);

        ParallelSaturation<EchoClosure, EchoAxiom, Integer> parallelSaturation = new ParallelSaturation<>(
                initializationFactory
        );

        EchoClosure closure = parallelSaturation.saturate();

        System.out.println(closure.getClosureResults());
    }

    @Test
    void testDistributedEchoSaturation() {
        int numWorkers = 4;
        SaturationInitializationFactory<EchoClosure, EchoAxiom, Integer> initializationFactory = new EchoSaturationInitializationFactory(
                numWorkers, 1000);

        SaturationWorkerThreadGenerator serverGen = new SaturationWorkerThreadGenerator(
                numWorkers
        );

        serverGen.generateAndRunWorkers();
        List<ServerData> serverDataList = serverGen.getWorkerServerDataList();


        DistributedSaturation<EchoClosure, EchoAxiom, Integer> distributedSaturation = new DistributedSaturation<>(
                initializationFactory.getDistributedWorkerModels(serverDataList),
                initializationFactory.getWorkloadDistributor(),
                initializationFactory.getInitialAxioms(),
                initializationFactory.getNewClosure()
        );

        EchoClosure closure = distributedSaturation.saturate();

        System.out.println(closure.getClosureResults());

        serverGen.stopWorkers();
    }
}
