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

        List<Long> socketID = new ArrayList<>();

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
                socketID.add(socketManager.getSocketID());
            }
        };

        ServerConnector serverConnector = new ServerConnector(new ServerData("localhost", serverPort)) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {

            }
        };


        NetworkingComponent networkingComponent = new NetworkingComponent(
                messageProcessor,
                Collections.singletonList(portListener),
                Collections.singletonList(serverConnector));
        networkingComponent.startNIOThread();

        while (socketID.isEmpty()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long id = socketID.iterator().next();
        for (int i = 0; i < 5; i++) {
            MessageEnvelope envelope = new MessageEnvelope(id, "Hello socket " + id + "! - " + LocalDateTime.now());
            networkingComponent.sendMessage(envelope);
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
