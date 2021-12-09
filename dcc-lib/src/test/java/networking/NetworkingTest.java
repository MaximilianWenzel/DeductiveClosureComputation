package networking;

import networking.connectors.PortListener;
import networking.connectors.ServerConnector;
import networking.io.MessageProcessor;
import networking.io.SocketManager;
import networking.io.SocketManagerFactory;
import networking.messages.MessageEnvelope;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class NetworkingTest {

    @Test
    public void testNIOServerCommunication() {
        int serverPort = 6066;

        List<Long> socketIDs = new ArrayList<>();

        MessageProcessor messageProcessor = new MessageProcessor() {
            @Override
            public void process(MessageEnvelope message) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(LocalDateTime.now() + " - Received message: " + message.getMessage());
            }
        };

        PortListener portListener = new PortListener(serverPort) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };

        ServerConnector serverConnector1 = new ServerConnector(new ServerData("localhost", serverPort)) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };

        ServerConnector serverConnector2 = new ServerConnector(new ServerData("localhost", serverPort)) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                socketIDs.add(socketManager.getSocketID());
            }
        };
        List<ServerConnector> serverConnectors = new ArrayList<>();
        serverConnectors.add(serverConnector1);
        serverConnectors.add(serverConnector2);


        NetworkingComponent networkingComponent = new NetworkingComponent(
                new SocketManagerFactory(),
                messageProcessor,
                Collections.singletonList(portListener),
                serverConnectors);
        networkingComponent.startNIOThread();

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

        ServerConnector serverConnector3 = new ServerConnector(new ServerData("localhost", serverPort)) {
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
            Scanner s = new Scanner(System.in);
            s.next();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
