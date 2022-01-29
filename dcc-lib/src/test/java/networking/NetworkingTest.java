package networking;

import benchmark.jmh.ReceiverStub;
import benchmark.jmh.SenderStub;
import networking.connectors.ConnectionEstablishmentListener;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import org.junit.jupiter.api.Test;
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

        ConnectionEstablishmentListener portListener = new ConnectionEstablishmentListener(serverData, messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };

        ConnectionEstablishmentListener serverConnector1 = new ConnectionEstablishmentListener(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };

        ConnectionEstablishmentListener serverConnector2 = new ConnectionEstablishmentListener(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };
        List<ConnectionEstablishmentListener> serverConnectors = new ArrayList<>();
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

        ConnectionEstablishmentListener serverConnector3 = new ConnectionEstablishmentListener(serverData,
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
        BlockingQueue<Object> arrayBlockingQueue = QueueFactory.createSaturationToDo();
        int numResults = 100;

        ReceiverStub receiverStub = new ReceiverStub(numResults);
        SenderStub senderStub = new SenderStub(new ServerData("localhost", receiverStub.getServerPort())
        );

        for (int i = 0; i < numResults; i++) {
            senderStub.sendMessage("Test");
        }
    }

    @Test
    void testNIO2NetworkCommunication() {
        ServerData serverData = new ServerData("localhost", NetworkingUtils.getFreePort());
        List<Long> socketIDs = new ArrayList<>();
        BlockingQueue<String> receivedMessages = QueueFactory.createSaturationToDo();
        MessageHandler messageHandler = new MessageHandler() {
            @Override
            public void process(long socketID, Object message) {
                try {
                    receivedMessages.put((String) message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(LocalDateTime.now() + " - Received message: " + message);
            }
        };

        ConnectionEstablishmentListener portListener = new ConnectionEstablishmentListener(serverData, messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Client connected to server socket.");
            }
        };

        ConnectionEstablishmentListener serverConnector1 = new ConnectionEstablishmentListener(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
                System.out.println("Connection to server established.");
            }
        };

        ConnectionEstablishmentListener serverConnector2 = new ConnectionEstablishmentListener(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };
        List<ConnectionEstablishmentListener> serverConnectors = new ArrayList<>();
        serverConnectors.add(serverConnector1);
        serverConnectors.add(serverConnector2);

        ExecutorService threadPool = Executors.newFixedThreadPool(3);
        NIO2NetworkingComponent networkingComponent = new NIO2NetworkingComponent(
                Collections.singletonList(portListener),
                serverConnectors,
                threadPool
        );

        while (socketIDs.isEmpty()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int numMessages = 0;
        for (long id : socketIDs) {
            for (int i = 0; i < 100; i++) {
                networkingComponent.sendMessage(id, "Hello socket " + id + "! - " + LocalDateTime.now());
                numMessages++;
            }
        }

        ConnectionEstablishmentListener serverConnector3 = new ConnectionEstablishmentListener(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Connection established!");
                networkingComponent.sendMessage(socketIDs.iterator().next(), "Message from new connection!");
            }
        };
        numMessages++;

        try {
            networkingComponent.connectToServer(serverConnector3);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(numMessages, receivedMessages.size());
    }
}
