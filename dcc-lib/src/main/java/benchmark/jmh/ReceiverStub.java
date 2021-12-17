package benchmark.jmh;

import networking.NIO2NetworkingComponent;
import networking.NetworkingComponent;
import networking.connectors.PortListener;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import util.NetworkingUtils;

import java.util.Collections;
import java.util.Queue;

public class ReceiverStub {

    int serverPort;
    NetworkingComponent networkingComponent;

    public ReceiverStub(Queue<Object> queue) {
        init(queue);
    }

    private void init(Queue<Object> queue) {
        MessageHandler messageHandler = new MessageHandler() {
            @Override
            public void process(long socketID, Object message) {
                assert message != null;
                queue.add(message);
            }
        };

        serverPort = NetworkingUtils.getFreePort();
        PortListener portListener = new PortListener(serverPort, messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Client connected.");
            }
        };

        networkingComponent = new NIO2NetworkingComponent(
                Collections.singletonList(portListener),
                Collections.emptyList()
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
    }
}
