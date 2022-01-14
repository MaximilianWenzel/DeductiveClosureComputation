package benchmark.jmh;

import enums.NetworkingComponentType;
import networking.NIO2NetworkingComponent;
import networking.NIONetworkingComponent;
import networking.NetworkingComponent;
import networking.ServerData;
import networking.connectors.ConnectionEstablishmentListener;
import networking.io.MessageHandler;
import networking.io.SocketManager;

import java.io.Serializable;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SenderStub {

    NetworkingComponent networkingComponent;
    ServerData serverData;
    SocketManager destinationSocket;
    NetworkingComponentType type;


    public SenderStub(ServerData serverData, NetworkingComponentType type) {
        this.serverData = serverData;
        this.type = type;
        init();
    }

    private void init() {
        MessageHandler messageHandler = new MessageHandler() {
            @Override
            public void process(long socketID, Object message) {
            }
        };


        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        AtomicInteger connectionEstablished = new AtomicInteger(0);
        ConnectionEstablishmentListener serverConnector = new ConnectionEstablishmentListener(serverData, messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                destinationSocket = socketManager;
                connectionEstablished.getAndIncrement();
                System.out.println("Connection to server established.");
            }
        };

        System.out.println("Connecting to server...");
        switch (type) {
            case NIO:
                networkingComponent = new NIONetworkingComponent(
                        Collections.emptyList(),
                        Collections.singletonList(serverConnector),
                        () -> {}
                );
                break;
            case ASYNC_NIO2:
                networkingComponent = new NIO2NetworkingComponent(
                        Collections.emptyList(),
                        Collections.singletonList(serverConnector),
                        Executors.newFixedThreadPool(1)
                );
                break;
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
