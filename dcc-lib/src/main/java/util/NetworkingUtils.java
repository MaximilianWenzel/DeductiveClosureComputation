package util;

import java.io.IOException;
import java.net.ServerSocket;

public class NetworkingUtils {

    public static int getFreePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }

    public static int getFreePortInPredefinedRange(int fromIncl, int toIncl) {
        for (int i = fromIncl; i <= toIncl; i++) {
            try (ServerSocket serverSocket = new ServerSocket(i)) {
                return serverSocket.getLocalPort();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        throw new IllegalStateException("All ports in range " + fromIncl + "-" + toIncl + " are already in use.");
    }
}
