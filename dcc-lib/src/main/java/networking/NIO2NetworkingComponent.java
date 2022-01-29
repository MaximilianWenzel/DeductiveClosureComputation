package networking;

import networking.connectors.ConnectionEstablishmentListener;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import networking.io.nio2.NIO2SocketManager;
import networking.messages.MessageEnvelope;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import util.ConsoleUtils;

import java.io.IOException;
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
import java.util.logging.Logger;

public class NIO2NetworkingComponent implements NetworkingComponent {

    protected List<ConnectionEstablishmentListener> portNumbersToListen;
    protected List<ConnectionEstablishmentListener> serversToConnectTo;

    protected ConcurrentMap<Long, NIO2SocketManager> socketIDToSocketManager = new ConcurrentHashMap<>();
    protected List<AsynchronousServerSocketChannel> serverSocketChannels = new ArrayList<>();

    protected AsynchronousChannelGroup asynchronousChannelGroup;
    protected ExecutorService threadPool;

    protected Subscriber<MessageEnvelope> messagesToSendSubscriber = new MessagesToSendSubscriber();
    protected Runnable callbackAfterAllMessagesHaveBeenSent;

    private Logger log = ConsoleUtils.getLogger();

    public NIO2NetworkingComponent(ExecutorService threadPool) {
        this.threadPool = threadPool;
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NIO2NetworkingComponent(List<ConnectionEstablishmentListener> portNumbersToListen,
                                   List<ConnectionEstablishmentListener> serversToConnectTo,
                                   ExecutorService threadPool) {
        this.portNumbersToListen = portNumbersToListen;
        this.serversToConnectTo = serversToConnectTo;
        this.threadPool = threadPool;
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() throws IOException {
        asynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(threadPool);

        for (ConnectionEstablishmentListener portListener : portNumbersToListen) {
            listenToPort(portListener);
        }

        for (ConnectionEstablishmentListener serverConnector : serversToConnectTo) {
            connectToServer(serverConnector);
        }
    }

    @Override
    public void listenToPort(ConnectionEstablishmentListener portListener) throws IOException {
        AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);

        ServerData serverData = portListener.getServerData();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(serverData.getHostname(),
                serverData.getPortNumber());
        log.info("Listening on " + inetSocketAddress + "...");
        server.bind(inetSocketAddress);
        server.accept(portListener.getMessageProcessor(), new ServerSocketCompletionHandler(portListener, server));
        this.serverSocketChannels.add(server);
    }

    public void connectToServer(ConnectionEstablishmentListener serverConnector) throws IOException {
        AsynchronousSocketChannel client = AsynchronousSocketChannel.open(asynchronousChannelGroup);
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
    public void sendMessage(long socketID, Object message) {
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

        for (AsynchronousServerSocketChannel socketChannel : serverSocketChannels) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        serverSocketChannels.clear();
    }

    @Override
    public void terminate() {
        closeAllSockets();
        this.asynchronousChannelGroup.shutdown();
    }

    @Override
    public void terminateAfterAllMessagesHaveBeenSent() {
        closeAllSockets();
    }

    public void setCallBackAfterAllMessagesHaveBeenSent(Runnable callbackAfterAllMessagesHaveBeenSent) {
        this.callbackAfterAllMessagesHaveBeenSent = callbackAfterAllMessagesHaveBeenSent;
    }

    public Subscriber<MessageEnvelope> getSubscriberForMessagesToSend() {
        return messagesToSendSubscriber;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    private class MessagesToSendSubscriber implements Subscriber<MessageEnvelope>, Runnable {

        private final int NUM_ELEMENTS_TO_REQUEST = 1;
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            if (allSocketsCanWrite()) {
                subscription.request(NUM_ELEMENTS_TO_REQUEST);
            }
        }

        @Override
        public void onNext(MessageEnvelope messageEnvelope) {
            sendMessage(messageEnvelope.getSocketID(), messageEnvelope.getMessage());
            if (allSocketsCanWrite()) {
                subscription.request(NUM_ELEMENTS_TO_REQUEST);
            } else {
                threadPool.submit(this);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            System.err.println(throwable.getMessage());
            throwable.printStackTrace();
        }

        @Override
        public void onComplete() {
            if (NIO2NetworkingComponent.this.callbackAfterAllMessagesHaveBeenSent != null) {
                NIO2NetworkingComponent.this.callbackAfterAllMessagesHaveBeenSent.run();
            }
        }

        private boolean allSocketsCanWrite() {
            return socketIDToSocketManager.values().stream()
                    .allMatch(NIO2SocketManager::canWriteMessages);
        }

        @Override
        public void run() {
            if (allSocketsCanWrite()) {
                subscription.request(NUM_ELEMENTS_TO_REQUEST);
            } else {
                threadPool.submit(this);
            }
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
            NIO2SocketManager socketManager = new NIO2SocketManager(socket, (MessageHandler) attachment
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
