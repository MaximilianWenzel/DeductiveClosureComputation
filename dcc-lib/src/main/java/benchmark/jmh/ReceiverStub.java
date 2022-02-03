package benchmark.jmh;

import enums.NetworkingComponentType;
import networking.NIO2NetworkingComponent;
import networking.NIONetworkingComponent;
import networking.NetworkingComponent;
import networking.ServerData;
import networking.connectors.ConnectionModel;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import util.NetworkingUtils;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReceiverStub {

    private int serverPort = -1;
    private String hostname;
    private NetworkingComponent networkingComponent;
    private NetworkingComponentType type;
    private ExecutorService threadPool;

    public ReceiverStub(NetworkingComponentType type) {
        this.type = type;
        init();
    }

    private void init() {
        MessageHandler messageHandler = new MessageHandler() {
            @Override
            public void process(long socketID, Object message) {
                assert message.hashCode() > 0;
            }
        };

        hostname = "localhost";
        if (serverPort == -1) {
            serverPort = NetworkingUtils.getFreePort();
        }
        ConnectionModel portListener = new ConnectionModel(
                new ServerData(hostname, serverPort), messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Client connected.");
                networkingComponent.closeServerSockets();
            }
        };

        switch (type) {
            case NIO:
                networkingComponent = new NIONetworkingComponent(
                        () -> {
                        }
                );
                break;
            case ASYNC_NIO2:
                threadPool = Executors.newFixedThreadPool(1);
                networkingComponent = new NIO2NetworkingComponent(threadPool);
                break;
        }

        try {
            networkingComponent.listenOnPort(portListener);
        } catch (IOException e) {
            e.printStackTrace();
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
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }
}
