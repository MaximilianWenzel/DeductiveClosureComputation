package benchmark.jmh;

import enums.NetworkingComponentType;
import networking.NIO2NetworkingComponent;
import networking.NetworkingComponent;
import networking.ServerData;
import networking.connectors.ConnectionEstablishmentListener;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import util.NetworkingUtils;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ReceiverStub {

    private int serverPort = -1;
    private String hostname;
    private NetworkingComponent networkingComponent;
    private ExecutorService threadPool;
    private int hashSum = 0;
    private AtomicLong receivedMessages = new AtomicLong(0);
    private long numberOfExpectedMessagesToReceive;
    private Runnable onAllMessagesReceived = () -> {};

    public ReceiverStub(long numberOfExpectedMessagesToReceive) {
        this.numberOfExpectedMessagesToReceive = numberOfExpectedMessagesToReceive;
        init();
    }

    public void setOnAllMessagesReceived(Runnable onAllMessagesReceived) {
        this.onAllMessagesReceived = onAllMessagesReceived;
    }

    private void init() {
        MessageHandler messageHandler = new MessageHandler() {
            @Override
            public void process(long socketID, Object message) {
                assert message.hashCode() > 0;
                increaseHashSum(message);
                receivedMessages.incrementAndGet();
                if (receivedMessages.get() == numberOfExpectedMessagesToReceive) {
                    onAllMessagesReceived.run();
                }
            }
        };

        hostname = "localhost";
        if (serverPort == -1) {
            serverPort = NetworkingUtils.getFreePort();
        }
        ConnectionEstablishmentListener portListener = new ConnectionEstablishmentListener(
                new ServerData(hostname, serverPort), messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Client connected.");
            }
        };

        threadPool = Executors.newFixedThreadPool(1);
        networkingComponent = new NIO2NetworkingComponent(
                Collections.singletonList(portListener),
                Collections.emptyList(),
                threadPool
        );

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int getServerPort() {
        return serverPort;
    }

    public void terminate() {
        networkingComponent.terminate();
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }

    public void increaseHashSum(Object obj) {
        this.hashSum += obj.hashCode();
    }

    public int getHashSum() {
        return hashSum;
    }
}
