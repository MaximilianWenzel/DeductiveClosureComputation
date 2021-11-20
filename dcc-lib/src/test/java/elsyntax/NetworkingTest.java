package elsyntax;

import networking.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
                System.out.println("Received message: " + message.getMessage());
            }
        };

        ClientConnectionListener clientConnectionListener = new ClientConnectionListener() {
            @Override
            public void newClientConnected(SocketManager socketManager) {
                try {
                    System.out.println("Client connected: " + socketManager.getSocketChannel().getRemoteAddress());
                    socketID.add(socketManager.getSocketID());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        NetworkingComponent networkingComponent = new NetworkingComponent(
                messageProcessor, clientConnectionListener,
                Collections.singletonList(serverPort),
                Collections.singletonList(new ServerData("localhost", serverPort)));
        networkingComponent.startNIOThread();

        while (socketID.isEmpty()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long id = socketID.iterator().next();
        MessageEnvelope envelope = new MessageEnvelope(id, "Hello socket " + id + "!");

        for (int i = 0; i < 5; i++) {
            networkingComponent.sendMessage(envelope);
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
