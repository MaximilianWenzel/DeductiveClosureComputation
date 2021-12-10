package benchmark.jmh;

import networking.NetworkingComponent;
import networking.ServerData;
import networking.connectors.ServerConnector;
import networking.io.MessageProcessor;
import networking.io.SocketManager;
import networking.io.SocketManagerFactory;
import networking.messages.MessageEnvelope;

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
        networkingComponent = new NetworkingComponent(
                new SocketManagerFactory(),
                new MessageProcessor() {
                    @Override
                    public void process(long socketID, Object message) {
                    }
                },
                Collections.emptyList(),
                Collections.emptyList()
        );
        networkingComponent.startNIOThread();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        AtomicInteger connectionEstablished = new AtomicInteger(0);
        ServerConnector serverConnector = new ServerConnector(serverData) {
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
