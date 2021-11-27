package networking;

import networking.connectors.PortListener;
import networking.connectors.ServerConnector;
import util.ConsoleUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.Logger;

public class NetworkingComponent implements Runnable {

    protected Logger log = ConsoleUtils.getLogger();

    protected Thread nioThread;

    protected Selector selector;

    protected List<PortListener> portNumbersToListen;
    protected List<ServerConnector> serversToConnectTo;

    protected List<ServerSocketChannel> serverSocketChannels = new ArrayList<>();
    protected Queue<SocketManager> newConnectedSocketsQueue = new LinkedList<>();

    protected Map<Long, SocketManager> socketIDToMessageManager = new HashMap<>();

    protected MessageProcessor messageProcessor;

    protected boolean running = true;

    public NetworkingComponent(MessageProcessor messageProcessor,
                               List<PortListener> portNumbersToListen,
                               List<ServerConnector> serversToConnectTo) {
        this.portNumbersToListen = portNumbersToListen;
        this.serversToConnectTo = serversToConnectTo;

        this.messageProcessor = messageProcessor;
        init();
    }

    private void init() {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.serverSocketChannels = new ArrayList<>(portNumbersToListen.size());

        try {
            for (PortListener portListener : portNumbersToListen) {
                listenToPort(portListener);
            }

            for (ServerConnector serverConnector : this.serversToConnectTo) {
                connectToServer(serverConnector);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void listenToPort(PortListener portListener) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        key.attach(portListener);
        serverSocketChannel.bind(new InetSocketAddress(portListener.getPort()));
        this.serverSocketChannels.add(serverSocketChannel);
        log.info("Listening on port " + portListener + "...");
    }

    public void startNIOThread() {
        if (nioThread == null) {
            nioThread = new Thread(this);
            nioThread.start();
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                mainNIOSelectorLoop();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mainNIOSelectorLoop() throws IOException {
        selector.select();
        Set<SelectionKey> keys = selector.selectedKeys();
        for (SelectionKey key : keys) {
            if (key.isReadable()) {
                readFromSocket(key);
            }
            if (key.isWritable()) {
                writeToSocket(key);
            }
            if (key.isAcceptable()) {
                acceptClientConnection(key);
            }
            if (key.isConnectable()) {
                connectToServer(key);
            }
            initNewConnectedSockets();
        }
        keys.clear();
    }

    private void connectToServer(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        if (!socketChannel.finishConnect()) {
            throw new IllegalStateException("Could not connect to server: " + socketChannel.getRemoteAddress());
        }
        SocketManager socketManager = new SocketManager(socketChannel);
        ServerConnector serverConnector = (ServerConnector) key.attachment();
        serverConnector.onConnectionEstablished(socketManager);

        this.newConnectedSocketsQueue.add(socketManager);
    }

    public void connectToServer(ServerConnector serverConnector) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        SelectionKey key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
        key.attach(serverConnector);
        ServerData serverData = serverConnector.getServerData();
        socketChannel.connect(new InetSocketAddress(serverData.getServerName(), serverData.getPortNumber()));
    }

    private void initNewConnectedSockets() throws IOException {
        while (!this.newConnectedSocketsQueue.isEmpty()) {
            SocketManager socketManager = newConnectedSocketsQueue.poll();
            this.socketIDToMessageManager.put(socketManager.getSocketID(), socketManager);

            SocketChannel socketChannel = socketManager.getSocketChannel();
            socketChannel.configureBlocking(false);
            SelectionKey key = socketChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            key.attach(socketManager);
        }
    }

    private void readFromSocket(SelectionKey key) throws IOException {
        SocketManager socketManager = (SocketManager) key.attachment();
        Queue<Object> receivedMessages = socketManager.readMessages();
        while (!receivedMessages.isEmpty()) {
            MessageEnvelope messageEnvelope = new MessageEnvelope(socketManager.getSocketID(), receivedMessages.poll());
            messageProcessor.process(messageEnvelope);
        }

        // connected socket is closed
        if (socketManager.endOfStreamReached()) {
            this.socketIDToMessageManager.remove(socketManager.getSocketID());
            key.attach(null);
            key.cancel();
            key.channel().close();
        }
    }


    private void writeToSocket(SelectionKey key) {
        SocketManager socketManager = (SocketManager) key.attachment();

        if (socketManager.hasMessagesToSend()) {
            socketManager.sendMessages();
        } else {
            // TODO remove write as interest operation
        }
    }

    public void sendMessage(MessageEnvelope messageEnvelope) {
        SocketManager socketManager = this.socketIDToMessageManager.get(messageEnvelope.getSocketID());
        try {
            if (socketManager != null) {
                socketManager.enqueueMessageToSend(messageEnvelope.getMessage());
            } else {
                throw new IllegalArgumentException("Socket with given ID does not exist: " + messageEnvelope.getSocketID());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void acceptClientConnection(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        while (socketChannel != null) {
            SocketManager socketManager = new SocketManager(socketChannel);
            this.newConnectedSocketsQueue.add(socketManager);
            log.info("Client connected: " + socketChannel.socket().getRemoteSocketAddress());

            PortListener portListener = (PortListener) key.attachment();
            portListener.onConnectionEstablished(socketManager);

            socketChannel = serverSocketChannel.accept();
        }
    }

    public void terminate() {
        // TODO close all connections
        this.running = false;
        try {
            for (ServerSocketChannel serverSocketChannel : serverSocketChannels) {
                serverSocketChannel.close();
            }

            for (SocketManager socketManager : this.socketIDToMessageManager.values()) {
                socketManager.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMessageProcessor(MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }
}
