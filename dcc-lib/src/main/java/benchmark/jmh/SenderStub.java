package benchmark.jmh;

import networking.NIO2NetworkingComponent;
import networking.NetworkingComponent;
import networking.ServerData;
import networking.connectors.ServerConnector;
import networking.io.MessageHandler;
import networking.io.SocketManager;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class SenderStub {

    NetworkingComponent networkingComponent;
    ServerData serverData;
    SocketManager destinationSocket;

    public SenderStub(ServerData serverData) {
        this.serverData = serverData;
        init();
    }

    private void init() {
        MessageHandler messageHandler = new MessageHandler() {
            @Override
            public void process(long socketID, Object message) {
            }
        };

        networkingComponent = new NIO2NetworkingComponent(
                Collections.emptyList(),
                Collections.emptyList()
        );

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        AtomicInteger connectionEstablished = new AtomicInteger(0);
        ServerConnector serverConnector = new ServerConnector(serverData, messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                destinationSocket = socketManager;
                connectionEstablished.getAndIncrement();
                System.out.println("Connection to server established.");
            }
        };
        try {
            System.out.println("Connecting to server...");
            networkingComponent.connectToServer(serverConnector);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // wait until connection established
        while (connectionEstablished.get() == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(Serializable obj) {
        networkingComponent.sendMessage(destinationSocket.getSocketID(), obj);
    }

    public void terminate() {
        networkingComponent.terminate();
    }
}
