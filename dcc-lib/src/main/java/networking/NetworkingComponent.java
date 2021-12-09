package networking;

import networking.connectors.PortListener;
import networking.connectors.ServerConnector;
import networking.io.MessageProcessor;
import networking.io.SocketManager;
import networking.io.SocketManagerFactory;
import networking.messages.MessageEnvelope;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.Logger;

public class NetworkingComponent implements Runnable {

    protected Thread nioThread;

    protected Selector selector;

    protected List<PortListener> portNumbersToListen;
    protected List<ServerConnector> serversToConnectTo;

    protected List<ServerSocketChannel> serverSocketChannels = new ArrayList<>();

    protected Map<Long, SocketManager> socketIDToMessageManager = new HashMap<>();

    protected MessageProcessor messageProcessor;
    protected SocketManagerFactory socketManagerFactory;

    final Object serverConnectorLock = new Object();

    protected boolean running = true;

    public NetworkingComponent(SocketManagerFactory socketManagerFactory,
                                MessageProcessor messageProcessor,
                               List<PortListener> portNumbersToListen,
                               List<ServerConnector> serversToConnectTo) {
        this.socketManagerFactory = socketManagerFactory;
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

    }

    public void listenToPort(PortListener portListener) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        key.attach(portListener);
        serverSocketChannel.bind(new InetSocketAddress(portListener.getPort()));
        this.serverSocketChannels.add(serverSocketChannel);
    }

    public Thread startNIOThread() {
        if (nioThread == null) {
            nioThread = new Thread(this);
            nioThread.start();
        }
        return nioThread;
    }

    @Override
    public void run() {
        try {
            for (PortListener portListener : portNumbersToListen) {
                listenToPort(portListener);
            }

            for (ServerConnector serverConnector : this.serversToConnectTo) {
                connectToServer(serverConnector);
            }

            while (running) {
                try {
                    mainNIOSelectorLoop();
                } catch(ClosedSelectorException e) {
                    // terminated
                } catch (CancelledKeyException e) {
                    // connection has been closed
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // close all channels
            for (ServerSocketChannel serverSocketChannel : serverSocketChannels) {
                SelectionKey key = serverSocketChannel.keyFor(selector);
                if (key != null) {
                    key.cancel();
                    key.channel().close();
                }
                serverSocketChannel.close();
            }

            for (SocketManager socketManager : this.socketIDToMessageManager.values()) {
                SelectionKey key = socketManager.getSocketChannel().keyFor(selector);
                if (key != null) {
                    key.cancel();
                    key.channel().close();
                }
                socketManager.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void mainNIOSelectorLoop() throws IOException, ClassNotFoundException {
        selector.select();
        if (!selector.isOpen()) {
            return;
        }
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
        }
        keys.clear();
    }

    private void connectToServer(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        if (!socketChannel.finishConnect()) {
            throw new IllegalStateException("Could not connect to server: " + socketChannel.getRemoteAddress());
        }
        SocketManager socketManager = socketManagerFactory.createNewSocketManager(socketChannel);

        synchronized (serverConnectorLock) {
            ServerConnector serverConnector = (ServerConnector) key.attachment();
            initNewConnectedSocket(socketManager);
            serverConnector.onConnectionEstablished(socketManager);
        }

    }


    public void connectToServer(ServerConnector serverConnector) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        synchronized (serverConnectorLock) {
            SelectionKey key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
            key.attach(serverConnector);
            ServerData serverData = serverConnector.getServerData();
            socketChannel.connect(new InetSocketAddress(serverData.getServerName(), serverData.getPortNumber()));
        }

        // finish connection establishment by NIO thread
        selector.wakeup();
    }

    private void initNewConnectedSocket(SocketManager socketManager) throws IOException {
        this.socketIDToMessageManager.put(socketManager.getSocketID(), socketManager);

        SocketChannel socketChannel = socketManager.getSocketChannel();
        socketChannel.configureBlocking(false);
        SelectionKey key = socketChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        key.attach(socketManager);
    }

    private void readFromSocket(SelectionKey key) throws IOException, ClassNotFoundException {
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

        synchronized (socketManager) {
            if (socketManager.hasMessagesToSend()) {
                socketManager.sendMessages();
            } else {
                // remove write selector
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            }
        }
    }

    public void sendMessage(long socketID, Serializable message) {
        SocketManager socketManager = this.socketIDToMessageManager.get(socketID);
        try {
            if (socketManager != null) {
                socketManager.enqueueMessageToSend(message);

                // add write selector
                synchronized (socketManager) {
                    SelectionKey key = socketManager.getSocketChannel().keyFor(selector);
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    selector.wakeup();
                }
            } else {
                throw new IllegalArgumentException("Socket with ID " + socketID + " does not exist.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void acceptClientConnection(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        while (socketChannel != null) {
            SocketManager socketManager = socketManagerFactory.createNewSocketManager(socketChannel);
            initNewConnectedSocket(socketManager);

            PortListener portListener = (PortListener) key.attachment();
            portListener.onConnectionEstablished(socketManager);

            socketChannel = serverSocketChannel.accept();
        }
    }

    public void terminate() {
        this.running = false;
        try {
            selector.close();
            this.nioThread.join();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean socketsCurrentlyReadMessages() {
        for (SocketManager socketManager : this.socketIDToMessageManager.values()) {
            if (socketManager.hasMessagesToRead()) {
                return true;
            }
        }
        return false;
    }

    public void setMessageProcessor(MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }
}
