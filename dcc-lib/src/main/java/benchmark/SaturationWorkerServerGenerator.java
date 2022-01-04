package benchmark;

import data.Closure;
import networking.ServerData;
import reasoning.saturation.distributed.SaturationWorker;
import util.NetworkingUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class SaturationWorkerServerGenerator<C extends Closure<A>, A extends Serializable, T extends Serializable> {

    private int numberOfWorkers;
    private List<ServerData> serverDataList;

    public SaturationWorkerServerGenerator(int numberOfWorkers) {
        this.numberOfWorkers = numberOfWorkers;
        init();
    }

    private void init() {
        serverDataList = new ArrayList<>();
        for (int i = 0; i < numberOfWorkers; i++) {
            serverDataList.add(new ServerData("localhost", NetworkingUtils.getFreePort()));
        }
    }


    public List<SaturationWorker<C, A, T>> generateWorkers() {
        List<SaturationWorker<C, A, T>> saturationWorkers = new ArrayList<>();
        for (ServerData serverData : serverDataList) {
            try {
                SaturationWorker<C, A, T> worker = new SaturationWorker<>(
                        serverData.getPortNumber(),
                        SaturationWorker.IncrementalReasonerType.SINGLE_THREADED
                );
                saturationWorkers.add(worker);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return saturationWorkers;
    }

    public List<ServerData> getServerDataList() {
        return serverDataList;
    }
}
