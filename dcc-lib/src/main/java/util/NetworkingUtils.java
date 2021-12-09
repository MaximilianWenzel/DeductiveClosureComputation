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
}
