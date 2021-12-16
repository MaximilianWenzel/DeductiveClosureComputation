package benchmark.jmh;

import networking.NIONetworkingComponent;
import networking.connectors.PortListener;
import networking.io.MessageProcessor;
import networking.io.SocketManager;
import util.NetworkingUtils;

import java.util.Collections;
import java.util.Queue;

public class ReceiverStub {

    int serverPort;
    NIONetworkingComponent networkingComponent;

    public ReceiverStub(Queue<Object> queue) {
        init(queue);
    }

    private void init(Queue<Object> queue) {
        MessageProcessor messageProcessor = new MessageProcessor() {
            @Override
            public void process(long socketID, Object message) {
                assert message != null;
                queue.add(message);
            }
        };

        serverPort = NetworkingUtils.getFreePort();
        PortListener portListener = new PortListener(serverPort, messageProcessor) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Client connected.");
            }
        };

        networkingComponent = new NIONetworkingComponent(
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
