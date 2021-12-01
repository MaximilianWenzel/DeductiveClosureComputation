package elsyntax;

import networking.*;
import networking.connectors.PortListener;
import networking.connectors.ServerConnector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
                MessageEnvelope envelope = new MessageEnvelope(id, "Hello socket " + id + "! - " + LocalDateTime.now());
                networkingComponent.sendMessage(envelope);
            }
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
