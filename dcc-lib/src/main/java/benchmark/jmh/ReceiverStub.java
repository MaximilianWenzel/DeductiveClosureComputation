package benchmark.jmh;

import enums.NetworkingComponentType;
import networking.NIO2NetworkingComponent;
import networking.NIONetworkingComponent;
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
    NetworkingComponentType type;

    public ReceiverStub(Queue<Object> queue, NetworkingComponentType type) {
        this.type = type;
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

        switch (type) {
            case NIO:
                networkingComponent = new NIONetworkingComponent(
                        Collections.singletonList(portListener),
                        Collections.emptyList()
                );
                break;
            case ASYNC_NIO:
                networkingComponent = new NIO2NetworkingComponent(
                        Collections.singletonList(portListener),
                        Collections.emptyList()
                );
                break;

        }

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
