package networking;

import benchmark.jmh.ReceiverStub;
import benchmark.jmh.SenderStub;
import networking.connectors.NIO2ConnectionModel;
import networking.connectors.NIOConnectionModel;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import networking.messages.MessageEnvelope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import util.NetworkingUtils;
import util.QueueFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NetworkingTest {

    @Test
    public void testNIOServerCommunication() {
        ServerData serverData = new ServerData("localhost", NetworkingUtils.getFreePort());

        List<Long> socketIDs = new ArrayList<>();

        MessageHandler messageHandler = new MessageHandler() {
            @Override
            public void process(long socketID, Object message) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(LocalDateTime.now() + " - Received message: " + message);
            }
        };

        NIOConnectionModel portListener = new NIOConnectionModel(serverData, messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };

        NIOConnectionModel serverConnector1 = new NIOConnectionModel(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };

        NIOConnectionModel serverConnector2 = new NIOConnectionModel(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };
        List<NIOConnectionModel> serverConnectors = new ArrayList<>();
        serverConnectors.add(serverConnector1);
        serverConnectors.add(serverConnector2);


        NIONetworkingComponent networkingComponent = new NIONetworkingComponent(
                Collections.singletonList(portListener),
                serverConnectors,
                () -> {
                }
        );
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        threadPool.submit(networkingComponent);

        while (socketIDs.isEmpty()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (long id : socketIDs) {
            for (int i = 0; i < 2; i++) {
                networkingComponent.sendMessage(id, "Hello socket " + id + "! - " + LocalDateTime.now());
            }
        }

        NIOConnectionModel serverConnector3 = new NIOConnectionModel(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Connection established!");
                networkingComponent.sendMessage(socketIDs.iterator().next(), "Message from new connection!");
            }
        };
        try {
            networkingComponent.connectToServer(serverConnector3);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSenderReceiverStubs() {
        int numResults = 100;

        ReceiverStub receiverStub = new ReceiverStub(numResults);
        SenderStub senderStub = new SenderStub(new ServerData("localhost", receiverStub.getServerPort())
        );

        Flux.range(1, numResults)
                .map(i -> new MessageEnvelope(senderStub.getDestinationSocket(), "Test"))
                .subscribe(senderStub.getNetworkingComponent().getNewSubscriberForMessagesToSend());
    }

    @Test
    void testNIO2NetworkCommunication() {
        ServerData serverData = new ServerData("localhost", NetworkingUtils.getFreePort());
        List<Long> socketIDs = new ArrayList<>();
        BlockingQueue<String> receivedMessages = QueueFactory.createSaturationToDo();


        NIO2ConnectionModel portListener = new NIO2ConnectionModel(serverData) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Client connected to server socket.");
            }
        };

        NIO2ConnectionModel serverConnector1 = new NIO2ConnectionModel(serverData) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
                System.out.println("Connection to server established.");
            }
        };

        NIO2ConnectionModel serverConnector2 = new NIO2ConnectionModel(serverData) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };
        List<NIO2ConnectionModel> serverConnectors = new ArrayList<>();
        serverConnectors.add(serverConnector1);
        serverConnectors.add(serverConnector2);

        ExecutorService threadPool = Executors.newFixedThreadPool(3);
        NIO2NetworkingComponent networkingComponent = new NIO2NetworkingComponent(
                threadPool,
                receivedMessagesPublisher -> {
                    Flux.from(receivedMessagesPublisher)
                            .subscribe(messageEnvelope -> {
                                if (messageEnvelope.getMessage() == null) {
                                    return;
                                }
                                try {
                                    receivedMessages.put((String) messageEnvelope.getMessage());
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                System.out.println(
                                        LocalDateTime.now() + " - Received message: " + messageEnvelope.getMessage());
                            });
                }
        );
        try {
            networkingComponent.listenToPort(portListener);
            for (NIO2ConnectionModel con : serverConnectors) {
                networkingComponent.connectToServer(con);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (socketIDs.isEmpty()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int numMessages = 100;
        long destinationSocket = socketIDs.iterator().next();

        Flux.range(1, numMessages)
                .map(i -> new MessageEnvelope(destinationSocket,
                        "Hello socket " + destinationSocket + "! - " + LocalDateTime.now()))
                .subscribe(networkingComponent.getNewSubscriberForMessagesToSend());


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(numMessages, receivedMessages.size());
    }
}
