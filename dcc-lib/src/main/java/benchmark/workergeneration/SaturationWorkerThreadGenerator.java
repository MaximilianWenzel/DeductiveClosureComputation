package benchmark.workergeneration;

import networking.ServerData;
import reasoning.saturation.distributed.SaturationWorker;
import util.NetworkingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SaturationWorkerThreadGenerator implements SaturationWorkerGenerator {

    private final int numberOfWorkers;
    private List<ServerData> serverDataList;
    private List<Thread> workerThreads = new ArrayList<>();
    private final List<SaturationWorker<?, ?, ?>> saturationWorkers = new ArrayList<>();
    private final int numberOfThreadsForSingleWorker;

    public SaturationWorkerThreadGenerator(int numberOfWorkers, int numberOfThreadsForSingleWorker) {
        this.numberOfWorkers = numberOfWorkers;
        this.numberOfThreadsForSingleWorker = numberOfThreadsForSingleWorker;
        init();
    }

    private void init() {
        serverDataList = new ArrayList<>();
        for (int i = 0; i < numberOfWorkers; i++) {
            serverDataList.add(new ServerData("localhost", NetworkingUtils.getFreePort()));
        }
    }


    public void generateAndRunWorkers() {
        for (ServerData serverData : serverDataList) {
            try {
                SaturationWorker<?, ?, ?> worker = new SaturationWorker<>(
                        serverData,
                        SaturationWorker.IncrementalReasonerType.SINGLE_THREADED,
                        numberOfThreadsForSingleWorker
                );
                saturationWorkers.add(worker);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        workerThreads = saturationWorkers.stream().map(Thread::new).collect(Collectors.toList());
        workerThreads.forEach(Thread::start);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopWorkers() {
        for (Thread workerThread : workerThreads) {
            try {
                workerThread.interrupt();
                workerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public List<ServerData> getWorkerServerDataList() {
        return serverDataList;
    }

}
