package networking;

import networking.connectors.NIO2ConnectionModel;
import networking.io.SocketManager;
import networking.io.nio2.NIO2SocketManager;
import networking.messages.MessageEnvelope;
import networking.messages.MessageModel;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import util.ConsoleUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.net.SocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NIO2NetworkingComponent {

    protected List<NIO2ConnectionModel> portNumbersToListen = new ArrayList<>();
    protected List<NIO2ConnectionModel> serversToConnectTo = new ArrayList<>();

    protected ConcurrentMap<Long, NIO2SocketManager> socketIDToSocketManager = new ConcurrentHashMap<>();
    protected List<AsynchronousServerSocketChannel> serverSocketChannels = new ArrayList<>();

    protected AsynchronousChannelGroup asynchronousChannelGroup;
    protected ExecutorService threadPool;

    // write messages to socket
    protected Set<MessagesToSendSubscriber> messagesToSendSubscribers = new UnifiedSet<>();
    protected AtomicBoolean allSocketsCanWrite = new AtomicBoolean(true);
    protected Consumer<Long> onSocketCanWriteMessages = (socketID) -> {
        if (!allSocketsCanWrite.get()) {
            // check if all sockets can write again
            if (allSocketsCanWrite()) {
                allSocketsCanWrite.set(true);
                for (MessagesToSendSubscriber subscriber : messagesToSendSubscribers) {
                    threadPool.submit(subscriber);
                }
            }
        }
    };

    // read messages from socket
    boolean receivedMessagesPipelineIsRunning = false;
    protected ReceivedMessagesPublisher receivedMessagesPublisher = new ReceivedMessagesPublisher();
    protected Consumer<ReceivedMessagesPublisher> onNewMessagesReceived;
    protected Runnable onSocketCanReadNewMessages = () -> {
        if (!receivedMessagesPipelineIsRunning) {
            receivedMessagesPipelineIsRunning = true;
            onNewMessagesReceived.accept(receivedMessagesPublisher);
        }
    };
    private Logger log = ConsoleUtils.getLogger();

    public NIO2NetworkingComponent(ExecutorService threadPool,
                                   Consumer<ReceivedMessagesPublisher> onNewMessagesReceived) {
        this.threadPool = threadPool;
        this.onNewMessagesReceived = onNewMessagesReceived;
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() throws IOException {
        asynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(threadPool);

        for (NIO2ConnectionModel portListener : portNumbersToListen) {
            listenToPort(portListener);
        }

        for (NIO2ConnectionModel serverConnector : serversToConnectTo) {
            connectToServer(serverConnector);
        }
    }

    public void listenToPort(NIO2ConnectionModel portListener) throws IOException {
        AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);

        ServerData serverData = portListener.getServerData();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(serverData.getHostname(),
                serverData.getPortNumber());
        log.info("Listening on " + inetSocketAddress + "...");
        server.bind(inetSocketAddress);
        server.accept(null, new ServerSocketCompletionHandler(portListener, server));
        this.serverSocketChannels.add(server);
    }

    public void connectToServer(NIO2ConnectionModel serverConnector) throws IOException {
        AsynchronousSocketChannel client = AsynchronousSocketChannel.open(asynchronousChannelGroup);
        ServerData serverData = serverConnector.getServerData();
        InetSocketAddress hostAddress = new InetSocketAddress(serverData.getHostname(), serverData.getPortNumber());
        try {
            log.info("Connecting to server: " + hostAddress);
            client.connect(hostAddress).get();
            NIO2SocketManager socketManager = new NIO2SocketManager(client, onSocketCanReadNewMessages,
                    onSocketCanWriteMessages);
            socketIDToSocketManager.put(socketManager.getSocketID(), socketManager);
            socketManager.startReading();
            serverConnector.onConnectionEstablished(socketManager);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(long socketID, Object message) {
        NIO2SocketManager socketManager = this.socketIDToSocketManager.get(socketID);
        if (socketManager == null) {
            throw new IllegalArgumentException("No socket exists with ID: " + socketID);
        }
        socketManager.sendMessage(message);
    }

    public boolean socketsCurrentlyReadMessages() {
        for (NIO2SocketManager socketManager : socketIDToSocketManager.values()) {
            if (socketManager.hasMessagesToRead()) {
                return true;
            }
        }
        return false;
    }

    public void closeSocket(long socketID) {
        SocketManager socketManager = this.socketIDToSocketManager.get(socketID);
        try {
            socketManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        socketIDToSocketManager.remove(socketID);
    }

    public void closeAllSockets() {
        for (AsynchronousServerSocketChannel socketChannel : serverSocketChannels) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        serverSocketChannels.clear();

        for (SocketManager socketManager : this.socketIDToSocketManager.values()) {
            try {
                socketManager.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socketIDToSocketManager.clear();
    }

    public void terminate() {
        closeAllSockets();
        asynchronousChannelGroup.shutdown();
    }

    public Subscriber<MessageEnvelope> getNewSubscriberForMessagesToSend() {
        return new MessagesToSendSubscriber();
    }

    public ReceivedMessagesPublisher getReceivedMessagesPublisher() {
        return receivedMessagesPublisher;
    }

    private boolean allSocketsCanWrite() {
        return socketIDToSocketManager.values().stream()
                .allMatch(NIO2SocketManager::canWriteMessages);
    }

    public class MessagesToSendSubscriber implements Subscriber<MessageEnvelope>, Runnable {

        private final int NUM_ELEMENTS_TO_REQUEST = 1;
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            messagesToSendSubscribers.add(this);
            if (allSocketsCanWrite()) {
                subscription.request(NUM_ELEMENTS_TO_REQUEST);
            }
        }

        @Override
        public void onNext(MessageEnvelope messageEnvelope) {
            sendMessage(messageEnvelope.getSocketID(), messageEnvelope.getMessage());
            // request next elements only if all sockets can write again
            if (allSocketsCanWrite()) {
                subscription.request(NUM_ELEMENTS_TO_REQUEST);
            } else {
                allSocketsCanWrite.set(false);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            System.err.println(throwable.getMessage());
            throwable.printStackTrace();
        }

        @Override
        public void onComplete() {
            messagesToSendSubscribers.remove(this);
        }

        @Override
        public void run() {
            if (allSocketsCanWrite()) {
                subscription.request(NUM_ELEMENTS_TO_REQUEST);
            } else {
                allSocketsCanWrite.set(false);
            }
        }
    }

    int count = 0;

    public class ReceivedMessagesPublisher implements Publisher<MessageEnvelope>, Subscription {
        private Subscriber<? super MessageEnvelope> subscriber;
        private Collection<NIO2SocketManager> socketManager = NIO2NetworkingComponent.this.socketIDToSocketManager.values();
        private long requestedMessages = 0L;

        @Override
        public void subscribe(Subscriber<? super MessageEnvelope> subscriber) {
            this.subscriber = subscriber;
            this.subscriber.onSubscribe(this);
        }

        @Override
        public void request(long newRequests) {
            if (subscriber == null) {
                // no subscriber
                return;
            }

            requestedMessages += newRequests;
            MessageEnvelope messageEnvelope;
            Object message;
            // TODO maybe implement fair policy for next message
            for (NIO2SocketManager sm : socketManager) {
                message = sm.readNextMessage();

                while (message != null && requestedMessages > 0) {
                    messageEnvelope = new MessageEnvelope(sm.getSocketID(), message);
                    subscriber.onNext(messageEnvelope);
                    requestedMessages--;
                    message = sm.readNextMessage();
                }
            }

            if (requestedMessages > 0) {
                // not enough messages available from sockets

                if (receivedMessagesPipelineIsRunning) {
                    // problem: reactor calls this method although 'onComplete' has been called before
                    subscriber.onNext(MessageEnvelope.EMPTY);
                }
                subscriber.onComplete();
                receivedMessagesPipelineIsRunning = false;
            }
        }

        @Override
        public void cancel() {
            subscriber = null;
        }

    }

    public class ServerSocketCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {
        private NIO2ConnectionModel portListener;
        private AsynchronousServerSocketChannel serverSocket;

        public ServerSocketCompletionHandler(NIO2ConnectionModel portListener,
                                             AsynchronousServerSocketChannel serverSocket) {
            this.portListener = portListener;
            this.serverSocket = serverSocket;
        }

        @Override
        public void completed(AsynchronousSocketChannel socket, Object attachment) {
            if (serverSocket.isOpen()) {
                serverSocket.accept(null, this);
            }
            NIO2SocketManager socketManager = new NIO2SocketManager(socket, onSocketCanReadNewMessages,
                    onSocketCanWriteMessages);
            socketIDToSocketManager.put(socketManager.getSocketID(), socketManager);
            socketManager.startReading();
            portListener.onConnectionEstablished(socketManager);
        }

        @Override
        public void failed(Throwable exc, Object attachment) {

        }
    }
}
