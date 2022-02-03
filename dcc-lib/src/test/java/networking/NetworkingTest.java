package networking;

import benchmark.jmh.ReceiverStub;
import benchmark.jmh.SenderStub;
import enums.NetworkingComponentType;
import networking.connectors.ConnectionModel;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import org.junit.jupiter.api.Test;
import util.NetworkingUtils;
import util.QueueFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

        ConnectionModel portListener = new ConnectionModel(serverData, messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };

        ConnectionModel serverConnector1 = new ConnectionModel(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };

        ConnectionModel serverConnector2 = new ConnectionModel(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };
        List<ConnectionModel> serverConnectors = new ArrayList<>();
        serverConnectors.add(serverConnector1);
        serverConnectors.add(serverConnector2);


        NIONetworkingComponent networkingComponent = new NIONetworkingComponent(() -> {});
        try {
            networkingComponent.listenOnPort(portListener);
            for (ConnectionModel serverConnector : serverConnectors) {
                networkingComponent.connectToServer(serverConnector);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        ConnectionModel serverConnector3 = new ConnectionModel(serverData,
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
        ReceiverStub receiverStub = new ReceiverStub(NetworkingComponentType.ASYNC_NIO2);
        SenderStub senderStub = new SenderStub(new ServerData("localhost", receiverStub.getServerPort()),
                NetworkingComponentType.ASYNC_NIO2);

        int numResults = 100;
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

        ConnectionModel portListener = new ConnectionModel(serverData, messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Client connected to server socket.");
            }
        };

        ConnectionModel serverConnector1 = new ConnectionModel(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
                System.out.println("Connection to server established.");
            }
        };

        ConnectionModel serverConnector2 = new ConnectionModel(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };
        List<ConnectionModel> serverConnectors = new ArrayList<>();
        serverConnectors.add(serverConnector1);
        serverConnectors.add(serverConnector2);

        ExecutorService threadPool = Executors.newFixedThreadPool(3);
        NIO2NetworkingComponent networkingComponent = new NIO2NetworkingComponent(
                threadPool
        );
        try {
            networkingComponent.listenOnPort(portListener);
            for (ConnectionModel serverConnector : serverConnectors) {
                networkingComponent.connectToServer(serverConnector);
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

        int numMessages = 0;
        for (long id : socketIDs) {
            for (int i = 0; i < 100; i++) {
                networkingComponent.sendMessage(id, "Hello socket " + id + "! - " + LocalDateTime.now());
                numMessages++;
            }
        }

        ConnectionModel serverConnector3 = new ConnectionModel(serverData,
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
