package benchmark.jmh;

import networking.NIO2NetworkingComponent;
import networking.ServerData;
import networking.connectors.NIO2ConnectionModel;
import networking.connectors.NIOConnectionModel;
import networking.io.SocketManager;
import networking.messages.MessageEnvelope;
import reactor.core.publisher.Flux;
import util.NetworkingUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class ReceiverStub {

    private int serverPort = -1;
    private String hostname;
    private NIO2NetworkingComponent networkingComponent;
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
        Consumer<MessageEnvelope> messageHandler = new Consumer<MessageEnvelope>() {
            @Override
            public void accept(MessageEnvelope messageEnvelope) {
                if (messageEnvelope.getMessage() == null) {
                    return;
                }
                Object message = messageEnvelope.getMessage();
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
        NIO2ConnectionModel portListener = new NIO2ConnectionModel(
                new ServerData(hostname, serverPort)) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Client connected.");
            }
        };

        threadPool = Executors.newFixedThreadPool(1);
        networkingComponent = new NIO2NetworkingComponent(
                threadPool,
                publisher -> {
                    Flux.from(networkingComponent.getReceivedMessagesPublisher())
                            .subscribe(messageHandler);
                }
        );
        try {
            networkingComponent.listenToPort(portListener);
        } catch (IOException e) {
            e.printStackTrace();
        }


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
