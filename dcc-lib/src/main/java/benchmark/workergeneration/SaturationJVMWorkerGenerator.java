package benchmark.workergeneration;

import networking.ServerData;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.Saturation;
import reasoning.saturation.distributed.SaturationWorker;
import util.NetworkingUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SaturationJVMWorkerGenerator implements SaturationWorkerGenerator {

    private int numberOfWorkers;
    private List<ServerData> serverDataList;
    private List<Process> workerProcesses = new ArrayList<>();
    private int numberOfThreadsForSingleWorker;

    public SaturationJVMWorkerGenerator(int numberOfWorkers, int numberOfThreadsForSingleWorker) {
        this.numberOfWorkers = numberOfWorkers;
        this.numberOfThreadsForSingleWorker = numberOfThreadsForSingleWorker;
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
                startWorkerJVM(serverData.getPortNumber());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startWorkerJVM(int portNumber) throws IOException, InterruptedException {
        String classpath = Arrays.stream(((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs())
                .map(URL::getFile)
                .collect(Collectors.joining(File.pathSeparator));
        String path = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder processBuilder = new ProcessBuilder(
                path,
                "-cp",
                classpath,
                SaturationWorker.class.getName(),
                "localhost", portNumber + "", numberOfThreadsForSingleWorker + "" // application args
        ).inheritIO();
        workerProcesses.add(processBuilder.start());
    }

    public List<ServerData> getWorkerServerDataList() {
        return serverDataList;
    }

    public void stopWorkers() {
        for (Process p : workerProcesses) {
            try {
                p.destroyForcibly().waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
