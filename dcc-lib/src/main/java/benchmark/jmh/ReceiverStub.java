package benchmark.jmh;

import networking.NetworkingComponent;
import networking.connectors.PortListener;
import networking.io.MessageProcessor;
import networking.io.SocketManager;
import networking.io.SocketManagerFactory;
import networking.messages.MessageEnvelope;
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
        serverPort = NetworkingUtils.getFreePort();
        PortListener portListener = new PortListener(serverPort) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Client connected.");
            }
        };
        networkingComponent = new NetworkingComponent(
                new SocketManagerFactory(),
                new MessageProcessor() {
                    @Override
                    public void process(long socketID, Object message) {
                        assert message != null;
                        queue.add(message);
                    }
                },
                Collections.singletonList(portListener),
                Collections.emptyList()
        );
        networkingComponent.startNIOThread();

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
