package util;

import benchmark.workergeneration.SaturationJVMWorkerGenerator;
import benchmark.workergeneration.SaturationWorkerGenerator;
import benchmark.workergeneration.SaturationWorkerThreadGenerator;
import data.Closure;
import enums.MessageDistributionType;
import networking.ServerData;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.SaturationInitializationFactory;
import reasoning.saturation.SingleThreadedSaturation;
import reasoning.saturation.distributed.DistributedSaturation;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.DistributedSaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.parallel.ParallelSaturation;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClosureComputationTestUtil {

    public static <C extends Closure<A>, A extends Serializable, T extends Serializable> Set<A> singleThreadedClosureComputation(
            SaturationInitializationFactory<C, A, T> factory) {

        SingleThreadedSaturation<C, A> saturation = new SingleThreadedSaturation<>(
                factory.getInitialAxioms(),
                factory.generateRules(),
                factory.getNewClosure()
        );

        C closure = saturation.saturate();
        Set<A> result = new UnifiedSet<>();
        result.addAll(closure.getClosureResults());
        //System.out.println("Closure: ");
        //result.forEach(System.out::println);
        return result;
    }

    public static <C extends Closure<A>, A extends Serializable, T extends Serializable> Set<A> parallelClosureComputation(
            SaturationInitializationFactory<C, A, T> factory) {
        ParallelSaturation<C, A, T> saturation = new ParallelSaturation<>(
                factory
        );

        C closure = saturation.saturate();
        Set<A> result = new UnifiedSet<>();
        result.addAll(closure.getClosureResults());

        assertEquals(singleThreadedClosureComputation(factory), result);
        return result;
    }


    public static <C extends Closure<A>, A extends Serializable, T extends Serializable> void distributedClosureComputation(
            SaturationInitializationFactory<C, A, T> factory,
            boolean workersInSeparateJVMs,
            int numberOfThreadsForSingleWorker, boolean sendAllMessagesOverNetwork) {
        List<ServerData> serverDataList = null;
        SaturationWorkerGenerator workerGenerator;
        int numberOfWorkers = factory.getWorkerModels().size();

        if (workersInSeparateJVMs) {
            workerGenerator = new SaturationJVMWorkerGenerator(numberOfWorkers, numberOfThreadsForSingleWorker);
            try {
                workerGenerator.generateAndRunWorkers();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } else {
            workerGenerator = new SaturationWorkerThreadGenerator(
                    numberOfWorkers, numberOfThreadsForSingleWorker);
            workerGenerator.generateAndRunWorkers();
        }
        serverDataList = workerGenerator.getWorkerServerDataList();


        List<DistributedWorkerModel<C, A, T>> workers = factory.getDistributedWorkerModels(
                serverDataList);

        MessageDistributionType messageDistributionType;
        if (sendAllMessagesOverNetwork) {
            messageDistributionType = MessageDistributionType.SEND_ALL_MESSAGES_OVER_NETWORK;
        } else {
            messageDistributionType = MessageDistributionType.ADD_OWN_MESSAGES_DIRECTLY_TO_TODO;
        }

        DistributedSaturationConfiguration configuration = new DistributedSaturationConfiguration(
                true,
                false,
                messageDistributionType
        );

        DistributedSaturation<C, A, T> saturation = new DistributedSaturation<>(
                workers,
                factory.getWorkloadDistributor(),
                factory.getInitialAxioms(),
                factory.getNewClosure(),
                configuration,
                1
        );

        C closure = saturation.saturate();
        Set<A> distributedResults = new UnifiedSet<>();
        distributedResults.addAll(closure.getClosureResults());

        //System.out.println("Closure: ");
        //closure.getClosureResults().forEach(System.out::println);

        Set<A> singleThreadedResults = singleThreadedClosureComputation(factory);
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
}
