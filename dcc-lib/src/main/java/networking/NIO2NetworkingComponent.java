package networking;

import networking.connectors.ConnectionEstablishmentListener;
import networking.connectors.ConnectionEstablishmentListener;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import networking.io.nio2.NIO2SocketManager;
import util.ConsoleUtils;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class NIO2NetworkingComponent implements NetworkingComponent {

    private Logger log = ConsoleUtils.getLogger();

    protected List<ConnectionEstablishmentListener> portNumbersToListen;
    protected List<ConnectionEstablishmentListener> serversToConnectTo;

    protected ConcurrentMap<Long, NIO2SocketManager> socketIDToSocketManager = new ConcurrentHashMap<>();
    protected AsynchronousChannelGroup threadPool;

    public NIO2NetworkingComponent(List<ConnectionEstablishmentListener> portNumbersToListen,
                                   List<ConnectionEstablishmentListener> serversToConnectTo) {
        this.portNumbersToListen = portNumbersToListen;
        this.serversToConnectTo = serversToConnectTo;
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() throws IOException {
        threadPool = AsynchronousChannelGroup.withFixedThreadPool(1, Thread::new);

        for (ConnectionEstablishmentListener portListener : portNumbersToListen) {
            listenToPort(portListener);
        }

        for (ConnectionEstablishmentListener serverConnector : serversToConnectTo) {
            connectToServer(serverConnector);
        }
    }

    @Override
    public void listenToPort(ConnectionEstablishmentListener portListener) throws IOException {
        AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(threadPool);

        ServerData serverData = portListener.getServerData();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(serverData.getHostname(), serverData.getPortNumber());
        log.info("Listening on " + inetSocketAddress + "...");
        server.bind(inetSocketAddress);
        server.accept(portListener.getMessageProcessor(), new ServerSocketCompletionHandler(portListener, server));
    }

    public void connectToServer(ConnectionEstablishmentListener serverConnector) throws IOException {
        AsynchronousSocketChannel client = AsynchronousSocketChannel.open(threadPool);
        ServerData serverData = serverConnector.getServerData();
        InetSocketAddress hostAddress = new InetSocketAddress(serverData.getHostname(), serverData.getPortNumber());
        /*
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

         */
        try {
            log.info("Connecting to server: " + hostAddress);
            client.connect(hostAddress).get();
            NIO2SocketManager socketManager = new NIO2SocketManager(client, serverConnector.getMessageProcessor());
            socketIDToSocketManager.put(socketManager.getSocketID(), socketManager);
            socketManager.startReading();
            serverConnector.onConnectionEstablished(socketManager);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMessage(long socketID, Serializable message) {
        NIO2SocketManager socketManager = this.socketIDToSocketManager.get(socketID);
        if (socketManager == null) {
            throw new IllegalArgumentException("No socket exists with ID: " + socketID);
        }
        socketManager.sendMessage(message);
    }

    @Override
    public void terminate() {
        try {
            threadPool.shutdownNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean socketsCurrentlyReadMessages() {
        for (NIO2SocketManager socketManager : socketIDToSocketManager.values()) {
            if (socketManager.hasMessagesToRead()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void closeSocket(long socketID) {
        SocketManager socketManager = this.socketIDToSocketManager.get(socketID);
        try {
            socketManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        socketIDToSocketManager.remove(socketID);
    }

    @Override
    public void closeAllSockets() {
        for (SocketManager socketManager : this.socketIDToSocketManager.values()) {
            try {
                socketManager.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socketIDToSocketManager.clear();
    }

    @Override
    public void terminateAfterAllMessagesHaveBeenSent() {
        try {
            threadPool.shutdown();
            threadPool.awaitTermination(5000, TimeUnit.MILLISECONDS);
            threadPool.shutdownNow();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private class ServerSocketCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

        private ConnectionEstablishmentListener portListener;
        private AsynchronousServerSocketChannel serverSocket;

        public ServerSocketCompletionHandler(ConnectionEstablishmentListener portListener,
                                             AsynchronousServerSocketChannel serverSocket) {
            this.portListener = portListener;
            this.serverSocket = serverSocket;
        }

        @Override
        public void completed(AsynchronousSocketChannel socket, Object attachment) {
            serverSocket.accept(portListener.getMessageProcessor(), this);
            NIO2SocketManager socketManager = new NIO2SocketManager(socket, (MessageHandler) attachment);
            socketIDToSocketManager.put(socketManager.getSocketID(), socketManager);
            socketManager.startReading();
            portListener.onConnectionEstablished(socketManager);
        }

        @Override
        public void failed(Throwable exc, Object attachment) {

        }
    }

}
