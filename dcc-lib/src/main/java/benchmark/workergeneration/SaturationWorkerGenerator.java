package benchmark.workergeneration;

import networking.ServerData;

import java.util.List;

public interface SaturationWorkerGenerator {

    void generateAndRunWorkers();

    List<ServerData> getWorkerServerDataList();

    void stopWorkers();
}
