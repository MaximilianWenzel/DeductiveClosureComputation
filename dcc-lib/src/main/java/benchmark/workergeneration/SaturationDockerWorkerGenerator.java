package benchmark.workergeneration;


import networking.ServerData;
import util.ConsoleUtils;
import util.NetworkingUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SaturationDockerWorkerGenerator implements SaturationWorkerGenerator {

    private static final Logger log = ConsoleUtils.getLogger();
    private static final String containerPrefix = "saturation-worker-";
    private final int numberOfWorkers;
    private List<ServerData> serverDataList;
    private List<String> containerNames;
    private List<Integer> portNumbers;
    private final String networkName = "saturation-worker-network";

    public SaturationDockerWorkerGenerator(int numberOfWorkers) {
        this.numberOfWorkers = numberOfWorkers;
        init();
    }

    private void init() {
        serverDataList = new ArrayList<>(numberOfWorkers);
        portNumbers = new ArrayList<>(numberOfWorkers);
        containerNames = new ArrayList<>(numberOfWorkers);

        for (int i = 0; i < numberOfWorkers; i++) {
            String containerName = containerPrefix + i;
            int port = NetworkingUtils.getFreePort();
            portNumbers.add(port);
            containerNames.add(containerName);
        }
    }

    public void generateAndRunWorkers() {
        createUserDefinedDockerNetworkForContainerDNS();
        try {
            for (int i = 0; i < numberOfWorkers; i++) {
                String ip = runWorker(containerNames.get(i), portNumbers.get(i));
                serverDataList.add(new ServerData(ip, portNumbers.get(i)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            log.info("Waiting until containers are initialized...");
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createUserDefinedDockerNetworkForContainerDNS() {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "docker", "network", "create", networkName
        ).inheritIO();
        try {
            processBuilder.start().waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public List<ServerData> getWorkerServerDataList() {
        return serverDataList;
    }

    /**
     * Returns the IP address of the resulting container.
     */
    private String runWorker(String containerName, int port) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "docker", "run",
                "--publish", port + ":" + port,
                "--expose", port + "",
                "--name", containerName,
                "--net", networkName,
                "--hostname", "0.0.0.0",
                "-d", "saturation-worker",
                "0.0.0.0", port + "" // application args: <HOSTNAME> <PORT>
        ).inheritIO();
        processBuilder.start();

        return containerName;
    }

    private String getDockerContainerIP(String containerName) {
        String argStr = "docker inspect --format \"{{ .NetworkSettings.IPAddress }}\" " + containerName;
        List<String> args = Arrays.stream(argStr.split(" "))
                .collect(Collectors.toList());

        ProcessBuilder processBuilder = new ProcessBuilder(args);

        try {
            Process p = processBuilder.start();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.waitFor();
            StringBuilder sb = new StringBuilder();
            String s = "";
            do {
                sb.append(s);
                s = bufferedReader.readLine();
            } while (s != null);

            String ip = sb.toString();
            System.out.println("Container IP: " + ip);
            return ip;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void stopWorkers() {
        log.info("Stopping docker containers...");
        for (String containerName : this.containerNames) {
            executeCommand("docker stop " + containerName);
            executeCommand("docker container rm " + containerName);
        }
    }

    private void executeCommand(String cmd) {
        List<String> args = Arrays.stream(cmd.split(" "))
                .collect(Collectors.toList());
        ProcessBuilder processBuilder = new ProcessBuilder(args).inheritIO();
        try {
            processBuilder.start().waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
