package networking;

import util.ConsoleUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public abstract class ServerComponent<P, T> extends NetworkingComponent<P, T> implements Runnable {

    protected Logger log = ConsoleUtils.getLogger();
    protected int portNumber;

    public ServerComponent(int portNumber) {
        this.portNumber = portNumber;
    }

    public ServerComponent() {
    }

    public void startListeningOnPort() {
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(this.portNumber)) {

            log.info("Listening on port " + portNumber + "...");
            Socket clientSocket = serverSocket.accept();
            init(clientSocket);
            log.info("Client connected: " + clientSocket.getRemoteSocketAddress());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
