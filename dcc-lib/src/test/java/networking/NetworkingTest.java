package networking;

import data.DefaultToDo;
import enums.NetworkingComponentType;
import benchmark.jmh.ReceiverStub;
import benchmark.jmh.SenderStub;
import networking.connectors.PortListener;
import networking.connectors.ServerConnector;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import org.junit.jupiter.api.Test;
import util.NetworkingUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        PortListener portListener = new PortListener(serverData, messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };

        ServerConnector serverConnector1 = new ServerConnector(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };

        ServerConnector serverConnector2 = new ServerConnector(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };
        List<ServerConnector> serverConnectors = new ArrayList<>();
        serverConnectors.add(serverConnector1);
        serverConnectors.add(serverConnector2);


        NIONetworkingComponent networkingComponent = new NIONetworkingComponent(
                Collections.singletonList(portListener),
                serverConnectors);

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

        ServerConnector serverConnector3 = new ServerConnector(serverData,
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
        BlockingQueue<Object> arrayBlockingQueue = new DefaultToDo<>();
        ReceiverStub receiverStub = new ReceiverStub(arrayBlockingQueue, NetworkingComponentType.ASYNC_NIO);
        SenderStub senderStub = new SenderStub(new ServerData("localhost", receiverStub.getServerPort()), NetworkingComponentType.ASYNC_NIO);

        int numResults = 100;
        for (int i = 0; i < numResults; i++) {
            senderStub.sendMessage("Test");
        }
    }

    @Test
    void testNIO2NetworkCommunication() {
        ServerData serverData = new ServerData("localhost", NetworkingUtils.getFreePort());
        List<Long> socketIDs = new ArrayList<>();
        BlockingQueue<String> receivedMessages = new DefaultToDo<>();
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

        PortListener portListener = new PortListener(serverData, messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Client connected to server socket.");
            }
        };

        ServerConnector serverConnector1 = new ServerConnector(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
                System.out.println("Connection to server established.");
            }
        };

        ServerConnector serverConnector2 = new ServerConnector(serverData,
                messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };
        List<ServerConnector> serverConnectors = new ArrayList<>();
        serverConnectors.add(serverConnector1);
        serverConnectors.add(serverConnector2);


        NIO2NetworkingComponent networkingComponent = new NIO2NetworkingComponent(
                Collections.singletonList(portListener),
                serverConnectors);

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

        ServerConnector serverConnector3 = new ServerConnector(serverData,
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
