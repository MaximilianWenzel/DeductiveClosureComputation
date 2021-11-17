package networking;

import util.ConsoleUtils;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

public abstract class ClientComponent<P, T> extends NetworkingComponent<P, T> implements Runnable {

    protected Logger log = ConsoleUtils.getLogger();
    protected int remotePortNumber;
    protected String serverName;

    public ClientComponent(String serverName, int remotePortNumber) {
        this.remotePortNumber = remotePortNumber;
        this.serverName = serverName;
    }

    public void connectToServer() {
        log.info("Connecting to server '" + serverName + "' on port " + remotePortNumber + "...");
        try {
            init(new Socket(serverName, remotePortNumber));
            log.info("Server connection established.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        connectToServer();
    }

    public int getRemotePortNumber() {
        return remotePortNumber;
    }

    public String getServerName() {
        return serverName;
    }
}
