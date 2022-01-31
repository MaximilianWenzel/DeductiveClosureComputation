package benchmark.workergeneration;

import networking.ServerData;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.distributed.SaturationWorker;
import util.NetworkingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SaturationWorkerThreadGenerator implements SaturationWorkerGenerator {

    private final int numberOfWorkers;
    private List<ServerData> serverDataList;
    private final List<SaturationWorker<?, ?, ?>> saturationWorkers = new ArrayList<>();

    public SaturationWorkerThreadGenerator(int numberOfWorkers) {
        this.numberOfWorkers = numberOfWorkers;
        init();
    }

    private void init() {
        serverDataList = new ArrayList<>();
        Set<Integer> freePorts = new UnifiedSet<>();
        for (int i = 0; i < numberOfWorkers; i++) {
            int freePort = 0;
            do {
                freePort = NetworkingUtils.getFreePort();
            } while (!freePorts.add(freePort));
            serverDataList.add(new ServerData("localhost", freePort));
        }
    }


    public void generateAndRunWorkers() {
        for (ServerData serverData : serverDataList) {
            try {
                SaturationWorker<?, ?, ?> worker = new SaturationWorker<>(
                        serverData
                );
                saturationWorkers.add(worker);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        saturationWorkers.forEach(SaturationWorker::start);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopWorkers() {
        saturationWorkers.forEach(SaturationWorker::stop);
    }

    public List<ServerData> getWorkerServerDataList() {
        return serverDataList;
    }

}
