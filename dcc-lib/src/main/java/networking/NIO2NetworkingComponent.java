package networking;

import networking.connectors.NIO2ConnectionModel;
import networking.connectors.NIOConnectionModel;
import networking.io.SocketManager;
import networking.io.nio2.NIO2SocketManager;
import networking.messages.MessageEnvelope;
import org.reactivestreams.Publisher;
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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class NIO2NetworkingComponent {

    protected List<NIO2ConnectionModel> portNumbersToListen;
    protected List<NIO2ConnectionModel> serversToConnectTo;

    protected ConcurrentMap<Long, NIO2SocketManager> socketIDToSocketManager = new ConcurrentHashMap<>();
    protected List<AsynchronousServerSocketChannel> serverSocketChannels = new ArrayList<>();

    protected AsynchronousChannelGroup asynchronousChannelGroup;
    protected ExecutorService threadPool;

    // write messages to socket
    protected MessagesToSendSubscriber messagesToSendSubscriber = new MessagesToSendSubscriber();
    protected Runnable callbackAfterAllMessagesHaveBeenSent;
    protected AtomicBoolean allSocketsCanWrite = new AtomicBoolean(true);
    protected Consumer<Long> onSocketCanWriteMessages = (socketID) -> {
        if (!allSocketsCanWrite.get()) {
            // check if all sockets can write again
            if (allSocketsCanWrite()) {
                allSocketsCanWrite.set(true);
                threadPool.submit(messagesToSendSubscriber);
            }
        }
    };

    // read messages from socket
    protected ReceivedMessagesPublisher receivedMessagesPublisher = new ReceivedMessagesPublisher();
    protected AtomicBoolean moreRequestedMessages = new AtomicBoolean(false);
    protected Runnable onSocketCanReadNewMessages = () -> {
        if (moreRequestedMessages.compareAndSet(true, false)) {
            threadPool.submit(receivedMessagesPublisher);
        }
    };

    private Logger log = ConsoleUtils.getLogger();

    public NIO2NetworkingComponent(ExecutorService threadPool) {
        this.threadPool = threadPool;
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NIO2NetworkingComponent(List<NIO2ConnectionModel> portNumbersToListen,
                                   List<NIO2ConnectionModel> serversToConnectTo,
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

    public void sendMessage(long socketID, Object message) {
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

    public void terminate() {
        closeAllSockets();
        this.asynchronousChannelGroup.shutdown();
    }

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
            if (allSocketsCanWrite()) {
                subscription.request(NUM_ELEMENTS_TO_REQUEST);
            }
        }

        int written = 0;

        @Override
        public void onNext(MessageEnvelope messageEnvelope) {
            sendMessage(messageEnvelope.getSocketID(), messageEnvelope.getMessage());
            // request next elements only if all sockets can write again
            if (allSocketsCanWrite()) {
                written++;
                subscription.request(NUM_ELEMENTS_TO_REQUEST);
            } else {
                written = 0;
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
            if (NIO2NetworkingComponent.this.callbackAfterAllMessagesHaveBeenSent != null) {
                NIO2NetworkingComponent.this.callbackAfterAllMessagesHaveBeenSent.run();
            }
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

    public class ReceivedMessagesPublisher implements Publisher<MessageEnvelope>, Subscription, Runnable {
        private Subscriber<? super MessageEnvelope> subscriber;
        private Collection<NIO2SocketManager> socketManager = NIO2NetworkingComponent.this.socketIDToSocketManager.values();
        private AtomicLong requestedMessages = new AtomicLong(0);

        @Override
        public void subscribe(Subscriber<? super MessageEnvelope> subscriber) {
            if (this.subscriber != null) {
                throw new IllegalArgumentException("Only one subscriber allowed for the given publisher.");
            }
            this.subscriber = subscriber;
            this.subscriber.onSubscribe(this);
        }

        @Override
        public void request(long requestedMsgs) {
            if (subscriber == null) {
                // no subscriber
                return;
            }

            this.requestedMessages.addAndGet(requestedMsgs);
            MessageEnvelope messageEnvelope;
            Object message;
            // TODO maybe implement fair policy for next message
            for (NIO2SocketManager sm : socketManager) {
                message = sm.readNextMessage();
                while (message != null && this.requestedMessages.get() > 0) {
                    messageEnvelope = new MessageEnvelope(sm.getSocketID(), message);
                    subscriber.onNext(messageEnvelope);
                    this.requestedMessages.decrementAndGet();
                    message = sm.readNextMessage();
                }
            }
            if (this.requestedMessages.get() > 0) {
                moreRequestedMessages.set(true);
                // not enough messages available from sockets
                // TODO probably occurs too often
                subscriber.onNext(MessageEnvelope.EMPTY);
                // resume when messages available again
            }
        }

        @Override
        public void cancel() {
            subscriber = null;
        }

        @Override
        public void run() {
            // resume with current number requested messages
            request(0);
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
            serverSocket.accept(null, this);
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
