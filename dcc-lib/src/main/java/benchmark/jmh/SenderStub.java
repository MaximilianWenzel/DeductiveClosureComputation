package benchmark.jmh;

import enums.NetworkingComponentType;
import networking.NIO2NetworkingComponent;
import networking.NIONetworkingComponent;
import networking.NetworkingComponent;
import networking.ServerData;
import networking.connectors.ConnectionModel;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import networking.messages.MessageEnvelope;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SenderStub {

    NetworkingComponent networkingComponent;
    ServerData serverData;
    SocketManager destinationSocket;
    NetworkingComponentType type;

    // used in case of NIO2
    Consumer<MessageEnvelope> onMessageCouldNotBeSent;
    ExecutorService threadPool;


    public SenderStub(ServerData serverData, NetworkingComponentType type) {
        this.serverData = serverData;
        this.type = type;
        this.threadPool = null;
        init();
    }

    public SenderStub(ServerData serverData, Consumer<MessageEnvelope> onMessageCouldNotBeSent, ExecutorService threadPool) {
        this.serverData = serverData;
        this.type = NetworkingComponentType.ASYNC_NIO2;
        this.onMessageCouldNotBeSent = onMessageCouldNotBeSent;
        this.threadPool = threadPool;
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
        ConnectionModel serverConnector = new ConnectionModel(serverData, messageHandler) {
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
                        () -> {}
                );
                break;
            case ASYNC_NIO2:
                if (threadPool == null) {
                    threadPool = Executors.newFixedThreadPool(1);
                }
                networkingComponent = new NIO2NetworkingComponent(
                        threadPool,
                        onMessageCouldNotBeSent,
                        (socketID) -> {}
                );
                break;
        }

        try {
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
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }
}
