package benchmark.workergeneration;

import networking.ServerData;

import java.util.List;

/**
 * This class can be used in order to initialize all workers for a given parallelized deductive closure computation procedure.
 */
public interface SaturationWorkerGenerator {

    void generateAndRunWorkers();

    List<ServerData> getWorkerServerDataList();

    void stopWorkers();
}
