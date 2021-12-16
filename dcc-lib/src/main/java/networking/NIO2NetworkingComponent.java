package networking;

import networking.connectors.PortListener;
import networking.connectors.ServerConnector;
import networking.io.MessageProcessor;
import networking.io.nio2.NIO2SocketManager;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NIO2NetworkingComponent implements NetworkingComponent {

    protected List<PortListener> portNumbersToListen;
    protected List<ServerConnector> serversToConnectTo;

    protected ConcurrentMap<Long, NIO2SocketManager> socketIDToSocketManager = new ConcurrentHashMap<>();
    protected AsynchronousChannelGroup threadPool;

    public NIO2NetworkingComponent(List<PortListener> portNumbersToListen,
                                   List<ServerConnector> serversToConnectTo) {
        this.portNumbersToListen = portNumbersToListen;
        this.serversToConnectTo = serversToConnectTo;
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() throws IOException {
        threadPool = AsynchronousChannelGroup.withFixedThreadPool(2, Thread::new);

        for (PortListener portListener : portNumbersToListen) {
            listenToPort(portListener);
        }

        for (ServerConnector serverConnector : serversToConnectTo) {
            connectToServer(serverConnector);
        }
    }

    @Override
    public void listenToPort(PortListener portListener) throws IOException {
        AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(threadPool);
        server.bind(new InetSocketAddress("localhost", portListener.getPort()));
        server.accept(portListener.getMessageProcessor(), new ServerSocketCompletionHandler(portListener, server));
    }

    public void connectToServer(ServerConnector serverConnector) throws IOException {
        AsynchronousSocketChannel client = AsynchronousSocketChannel.open(threadPool);
        ServerData serverData = serverConnector.getServerData();
        InetSocketAddress hostAddress = new InetSocketAddress(serverData.getServerName(), serverData.getPortNumber());
        client.connect(hostAddress, serverConnector.getMessageProcessor(),
                new CompletionHandler<Void, MessageProcessor>() {
                    @Override
                    public void completed(Void result, MessageProcessor attachment) {
                        NIO2SocketManager socketManager = new NIO2SocketManager(client, attachment);
                        socketIDToSocketManager.put(socketManager.getSocketID(), socketManager);
                        serverConnector.onConnectionEstablished(socketManager);
                    }

                    @Override
                    public void failed(Throwable exc, MessageProcessor attachment) {
                    }
                });
    }

    @Override
    public void sendMessage(long socketID, Serializable message) {
        NIO2SocketManager socketManager = this.socketIDToSocketManager.get(socketID);
        if (socketManager == null) {
            throw new IllegalArgumentException();
        }
        try {
            socketManager.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void terminate() {
        threadPool.shutdown();

        this.socketIDToSocketManager.values().forEach(socketManager -> {
            try {
                socketManager.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        this.socketIDToSocketManager.clear();
    }

    @Override
    public boolean socketsCurrentlyReadMessages() {
        return false;
    }

    private class ServerSocketCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

        private PortListener portListener;
        private AsynchronousServerSocketChannel serverSocket;

        public ServerSocketCompletionHandler(PortListener portListener,
                                             AsynchronousServerSocketChannel serverSocket) {
            this.portListener = portListener;
            this.serverSocket = serverSocket;
        }

        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {
            NIO2SocketManager socketManager = new NIO2SocketManager(result, (MessageProcessor) attachment);
            socketIDToSocketManager.put(socketManager.getSocketID(), socketManager);
            portListener.onConnectionEstablished(socketManager);
            serverSocket.accept(portListener.getMessageProcessor(), this);
        }

        @Override
        public void failed(Throwable exc, Object attachment) {

        }
    }

}
