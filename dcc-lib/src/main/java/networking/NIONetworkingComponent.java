package networking;

import networking.connectors.ConnectionEstablishmentListener;
import networking.connectors.ConnectionEstablishmentListener;
import networking.io.SocketManager;
import networking.io.nio.NIOSocketManager;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class NIONetworkingComponent implements Runnable, NetworkingComponent {


    protected Logger log = ConsoleUtils.getLogger();
    protected Thread nioThread;

    protected Selector selector;

    protected List<ConnectionEstablishmentListener> portNumbersToListen;
    protected BlockingQueue<ConnectionEstablishmentListener> serversToConnectTo = new LinkedBlockingQueue<>();

    protected List<ServerSocketChannel> serverSocketChannels = new ArrayList<>();

    protected Map<Long, NIOSocketManager> socketIDToSocketManager = new HashMap<>();

    protected boolean running = true;

    public NIONetworkingComponent(List<ConnectionEstablishmentListener> portNumbersToListen,
                                  List<ConnectionEstablishmentListener> serversToConnectTo) {
        this.portNumbersToListen = portNumbersToListen;
        this.serversToConnectTo.addAll(serversToConnectTo);
        init();
    }

    private void init() {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.serverSocketChannels = new ArrayList<>(portNumbersToListen.size());
        startNIOThread();
    }

    @Override
    public void listenToPort(ConnectionEstablishmentListener portListener) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        key.attach(portListener);

        ServerData serverData = portListener.getServerData();
        serverSocketChannel.bind(new InetSocketAddress(serverData.getHostname(), serverData.getPortNumber()));
        this.serverSocketChannels.add(serverSocketChannel);
    }

    private Thread startNIOThread() {
        if (nioThread == null) {
            nioThread = new Thread(this);
            nioThread.start();
        }
        return nioThread;
    }

    @Override
    public void run() {
        try {
            for (ConnectionEstablishmentListener portListener : portNumbersToListen) {
                listenToPort(portListener);
            }

            while (!serversToConnectTo.isEmpty()) {
                connectToServer(serversToConnectTo.take(), SocketChannel.open());
            }

            while (running) {
                try {
                    mainNIOSelectorLoop();
                } catch (CancelledKeyException | IOException | ClosedSelectorException e) {
                    log.info("Connection got closed.");
                } catch (ClassNotFoundException e) {
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

            for (NIOSocketManager socketManager : this.socketIDToSocketManager.values()) {
                SelectionKey key = socketManager.getSocketChannel().keyFor(selector);
                if (key != null) {
                    key.cancel();
                    key.channel().close();
                }
                socketManager.close();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


    }

    private void mainNIOSelectorLoop() throws IOException, ClassNotFoundException, InterruptedException {
        selector.select();
        if (!selector.isOpen()) {
            return;
        }
        while (!serversToConnectTo.isEmpty()) {
            try {
                connectToServer(serversToConnectTo.take(), SocketChannel.open());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
                finishServerConnection(key);
            }
        }
        keys.clear();


    }

    private void finishServerConnection(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        socketChannel.finishConnect();

        ConnectionEstablishmentListener serverConnector = (ConnectionEstablishmentListener) key.attachment();
        NIOSocketManager socketManager = new NIOSocketManager(socketChannel, serverConnector.getMessageProcessor());
        initNewConnectedSocket(socketManager);
        serverConnector.onConnectionEstablished(socketManager);
    }

    private void connectToServer(ConnectionEstablishmentListener serverConnector, SocketChannel socketChannel) throws IOException {
        socketChannel.configureBlocking(false);

        ServerData serverData = serverConnector.getServerData();
        socketChannel.connect(new InetSocketAddress(serverData.getHostname(), serverData.getPortNumber()));
        SelectionKey key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
        key.attach(serverConnector);
        if (socketChannel.finishConnect()) {
            finishServerConnection(key);
        }
    }


    @Override
    public void connectToServer(ConnectionEstablishmentListener serverConnector) throws IOException {
        try {
            serversToConnectTo.put(serverConnector);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // finish connection establishment by NIO thread
        selector.wakeup();
    }

    private void initNewConnectedSocket(NIOSocketManager socketManager) throws IOException {
        this.socketIDToSocketManager.put(socketManager.getSocketID(), socketManager);

        SocketChannel socketChannel = socketManager.getSocketChannel();
        socketChannel.configureBlocking(false);
        SelectionKey key = socketChannel.register(this.selector, SelectionKey.OP_READ);
        key.attach(socketManager);
    }

    private void readFromSocket(SelectionKey key) throws IOException, ClassNotFoundException {
        NIOSocketManager socketManager = (NIOSocketManager) key.attachment();
        socketManager.readMessages();

        // connected socket is closed
        if (socketManager.endOfStreamReached()) {
            this.socketIDToSocketManager.remove(socketManager.getSocketID());
            key.attach(null);
            key.cancel();
            key.channel().close();
        }
    }

    private void writeToSocket(SelectionKey key) throws IOException {
        NIOSocketManager socketManager = (NIOSocketManager) key.attachment();

        if (socketManager.sendMessages()) {
            // all messages have been sent
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    @Override
    public void sendMessage(long socketID, Serializable message) {
        NIOSocketManager socketManager = this.socketIDToSocketManager.get(socketID);
        try {
            if (!socketManager.sendMessage(message)) {
                // has still messages to send - add write selector
                SelectionKey key = socketManager.getSocketChannel().keyFor(selector);
                if ((key.interestOps() & SelectionKey.OP_WRITE) == 0) {
                    // write is not set yet
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    selector.wakeup();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void acceptClientConnection(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        while (socketChannel != null) {
            ConnectionEstablishmentListener portListener = (ConnectionEstablishmentListener) key.attachment();

            NIOSocketManager socketManager = new NIOSocketManager(socketChannel, portListener.getMessageProcessor());
            initNewConnectedSocket(socketManager);

            portListener.onConnectionEstablished(socketManager);

            socketChannel = serverSocketChannel.accept();
        }
    }

    @Override
    public void terminate() {
        this.running = false;
        try {
            selector.close();
            this.nioThread.join();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean socketsCurrentlyReadMessages() {
        for (NIOSocketManager socketManager : this.socketIDToSocketManager.values()) {
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
        this.running = false;
        try {
            selector.close();
            this.nioThread.join();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
