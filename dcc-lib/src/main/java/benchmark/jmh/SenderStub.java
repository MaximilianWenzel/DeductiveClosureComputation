package benchmark.jmh;

import networking.NIO2NetworkingComponent;
import networking.ServerData;
import networking.connectors.ConnectionEstablishmentListener;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import networking.messages.MessageEnvelope;

import java.io.Serializable;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SenderStub {

    NIO2NetworkingComponent networkingComponent;
    ServerData serverData;
    SocketManager destinationSocket;
    ExecutorService threadPool;

    int hashSum = 0;


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


        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        AtomicInteger connectionEstablished = new AtomicInteger(0);
        ConnectionEstablishmentListener serverConnector = new ConnectionEstablishmentListener(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                destinationSocket = socketManager;
                connectionEstablished.getAndIncrement();
                System.out.println("Connection to server established.");
            }
        };

        System.out.println("Connecting to server...");
        this.threadPool = Executors.newFixedThreadPool(1);
        networkingComponent = new NIO2NetworkingComponent(
                Collections.emptyList(),
                Collections.singletonList(serverConnector),
                threadPool
        );


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

    public NIO2NetworkingComponent getNetworkingComponent() {
        return networkingComponent;
    }

    public void increaseHashSum(Object obj) {
        this.hashSum += obj.hashCode();
    }

    public int getHashSum() {
        return hashSum;
    }
}
