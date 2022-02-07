package networking;

import networking.connectors.ConnectionModel;
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
    protected Thread nioThread = null;

    protected Selector selector;

    protected List<ConnectionModel> portNumbersToListen;
    protected BlockingQueue<ConnectionModel> serversToConnectTo = new LinkedBlockingQueue<>();

    protected List<ServerSocketChannel> serverSocketChannels = new ArrayList<>();

    protected Map<Long, NIOSocketManager> socketIDToSocketManager = new HashMap<>();

    protected boolean running = true;

    protected Runnable runnableForMainNIOLoop;

    public NIONetworkingComponent(Runnable runnableForMainNIOLoop) {
        this.serversToConnectTo.addAll(serversToConnectTo);
        this.runnableForMainNIOLoop = runnableForMainNIOLoop;
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

    @Override
    public void listenOnPort(ConnectionModel portListener) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        key.attach(portListener);

        ServerData serverData = portListener.getServerData();
        serverSocketChannel.bind(new InetSocketAddress(serverData.getHostname(), serverData.getPortNumber()));
        this.serverSocketChannels.add(serverSocketChannel);
    }

    @Override
    public void run() {
        try {
            for (ConnectionModel portListener : portNumbersToListen) {
                listenOnPort(portListener);
            }

            while (!serversToConnectTo.isEmpty()) {
                connectToServer(serversToConnectTo.take(), SocketChannel.open());
            }

            while (running) {
                try {
                    runnableForMainNIOLoop.run();
                    mainNIOSelectorLoop();
                } catch (CancelledKeyException | IOException | ClosedSelectorException e) {
                    log.info("Connection got closed.");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            closeAllSockets();
        }
    }

    private void mainNIOSelectorLoop() throws IOException, ClassNotFoundException, InterruptedException {
        selector.selectNow();
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

        ConnectionModel serverConnector = (ConnectionModel) key.attachment();
        NIOSocketManager socketManager = new NIOSocketManager(socketChannel, serverConnector.getMessageProcessor());
        initNewConnectedSocket(socketManager);
        serverConnector.onConnectionEstablished(socketManager);
    }

    private void connectToServer(ConnectionModel serverConnector, SocketChannel socketChannel) throws
            IOException {
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
    public void connectToServer(ConnectionModel serverConnector) throws IOException {
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
    public boolean sendMessage(long socketID, Serializable message) {
        NIOSocketManager socketManager = this.socketIDToSocketManager.get(socketID);
        if (!socketManager.sendMessage(message)) {
            // has still messages to send - add write selector
            SelectionKey key = socketManager.getSocketChannel().keyFor(selector);
            if ((key.interestOps() & SelectionKey.OP_WRITE) == 0) {
                // write is not set yet
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                selector.wakeup();
            }
        }
        return true;
    }

    private void acceptClientConnection(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        while (socketChannel != null) {
            ConnectionModel portListener = (ConnectionModel) key.attachment();

            NIOSocketManager socketManager = new NIOSocketManager(socketChannel, portListener.getMessageProcessor());
            initNewConnectedSocket(socketManager);

            portListener.onConnectionEstablished(socketManager);

            socketChannel = serverSocketChannel.accept();
        }
    }

    @Override
    public void terminate() {
        this.running = false;
        closeAllSockets();
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
        closeServerSockets();
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
    public void closeServerSockets() {
        serverSocketChannels.forEach(ss -> {
            try {
                ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void startNIOThread() {
        if (nioThread == null) {
            nioThread = new Thread(this);
        }
        nioThread.start();
    }
}
