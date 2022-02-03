package networking;

import networking.connectors.ConnectionModel;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import networking.io.nio2.NIO2SocketManager;
import networking.messages.MessageEnvelope;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class NIO2NetworkingComponent implements NetworkingComponent {

    protected Consumer<MessageEnvelope> onMessageCouldNotBeSent;
    protected ConcurrentMap<Long, NIO2SocketManager> socketIDToSocketManager = new ConcurrentHashMap<>();
    protected AsynchronousChannelGroup asynchronousChannelGroup;
    protected ExecutorService threadPool;
    protected List<AsynchronousServerSocketChannel> serverSocketChannels = new ArrayList<>();
    private Logger log = ConsoleUtils.getLogger();
    private Consumer<Long> onSocketOutboundBufferHasSpace;

    public NIO2NetworkingComponent(ExecutorService threadPool, Consumer<MessageEnvelope> onMessageCouldNotBeSent,
                                   Consumer<Long> onSocketOutboundBufferHasSpace) {
        this.threadPool = threadPool;
        this.onMessageCouldNotBeSent = onMessageCouldNotBeSent;
        this.onSocketOutboundBufferHasSpace = onSocketOutboundBufferHasSpace;
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NIO2NetworkingComponent(ExecutorService threadPool) {
        this.threadPool = threadPool;
        // TODO: implement other default consumer method if messages cannot be send
        this.onMessageCouldNotBeSent = messageEnvelope -> threadPool.submit(
                () -> sendMessage(messageEnvelope.getSocketID(), messageEnvelope.getMessage())
        );
        this.onSocketOutboundBufferHasSpace = (socketID) -> {
        };
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() throws IOException {
        asynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(threadPool);
    }

    @Override
    public void listenOnPort(ConnectionModel portListener) throws IOException {
        AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);

        ServerData serverData = portListener.getServerData();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(serverData.getHostname(),
                serverData.getPortNumber());
        log.info("Listening on " + inetSocketAddress + "...");
        server.bind(inetSocketAddress);
        server.accept(portListener.getMessageProcessor(), new ServerSocketCompletionHandler(portListener, server));
        this.serverSocketChannels.add(server);
    }

    public void connectToServer(ConnectionModel serverConnector) throws IOException {
        AsynchronousSocketChannel client = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        ServerData serverData = serverConnector.getServerData();
        InetSocketAddress hostAddress = new InetSocketAddress(serverData.getHostname(), serverData.getPortNumber());

        try {
            log.info("Connecting to server: " + hostAddress);
            client.connect(hostAddress).get();
            NIO2SocketManager socketManager = new NIO2SocketManager(client, serverConnector.getMessageProcessor(),
                    onMessageCouldNotBeSent, onSocketOutboundBufferHasSpace);
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

        closeServerSockets();
    }

    @Override
    public void terminate() {
        closeAllSockets();
        try {
            asynchronousChannelGroup.shutdownNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public void closeServerSockets() {
        for (AsynchronousServerSocketChannel serverSocketChannel : this.serverSocketChannels) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        serverSocketChannels.clear();
    }


    private class ServerSocketCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {
        private ConnectionModel portListener;
        private AsynchronousServerSocketChannel serverSocket;

        public ServerSocketCompletionHandler(ConnectionModel portListener,
                                             AsynchronousServerSocketChannel serverSocket) {
            this.portListener = portListener;
            this.serverSocket = serverSocket;
        }

        @Override
        public void completed(AsynchronousSocketChannel socket, Object attachment) {
            serverSocket.accept(portListener.getMessageProcessor(), this);
            NIO2SocketManager socketManager = new NIO2SocketManager(
                    socket,
                    (MessageHandler) attachment,
                    onMessageCouldNotBeSent,
                    onSocketOutboundBufferHasSpace
            );
            socketIDToSocketManager.put(socketManager.getSocketID(), socketManager);
            socketManager.startReading();
            portListener.onConnectionEstablished(socketManager);
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
        }
    }

}
