package networking;

import networking.connectors.PortListener;
import networking.connectors.ServerConnector;
import networking.io.SocketManager;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkingComponent implements Runnable {

    protected Thread nioThread;

    protected Selector selector;

    protected List<PortListener> portNumbersToListen;
    protected BlockingQueue<ServerConnector> serversToConnectTo = new LinkedBlockingQueue<>();

    protected List<ServerSocketChannel> serverSocketChannels = new ArrayList<>();

    protected Map<Long, SocketManager> socketIDToMessageManager = new HashMap<>();

    protected boolean running = true;

    public NetworkingComponent(List<PortListener> portNumbersToListen,
                               List<ServerConnector> serversToConnectTo) {
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

            while (!serversToConnectTo.isEmpty()) {
                connectToServer(serversToConnectTo.take(), SocketChannel.open());
            }

            while (running) {
                try {
                    mainNIOSelectorLoop();
                } catch (ClosedSelectorException e) {
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
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


    }

    private void mainNIOSelectorLoop() throws IOException, ClassNotFoundException {
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

        ServerConnector serverConnector = (ServerConnector) key.attachment();
        SocketManager socketManager = new SocketManager(socketChannel, serverConnector.getMessageProcessor());
        initNewConnectedSocket(socketManager);
        serverConnector.onConnectionEstablished(socketManager);
    }

    private void connectToServer(ServerConnector serverConnector, SocketChannel socketChannel) throws IOException {
        socketChannel.configureBlocking(false);

        ServerData serverData = serverConnector.getServerData();
        socketChannel.connect(new InetSocketAddress(serverData.getServerName(), serverData.getPortNumber()));
        SelectionKey key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
        key.attach(serverConnector);
        if (socketChannel.finishConnect()) {
            finishServerConnection(key);
        }
    }


    public void connectToServer(ServerConnector serverConnector) throws IOException {
        serversToConnectTo.add(serverConnector);
        // finish connection establishment by NIO thread
        selector.wakeup();
    }

    private void initNewConnectedSocket(SocketManager socketManager) throws IOException {
        this.socketIDToMessageManager.put(socketManager.getSocketID(), socketManager);

        SocketChannel socketChannel = socketManager.getSocketChannel();
        socketChannel.configureBlocking(false);
        SelectionKey key = socketChannel.register(this.selector, SelectionKey.OP_READ);
        key.attach(socketManager);
    }

    private void readFromSocket(SelectionKey key) throws IOException, ClassNotFoundException {
        SocketManager socketManager = (SocketManager) key.attachment();
        socketManager.readMessages();

        // connected socket is closed
        if (socketManager.endOfStreamReached()) {
            this.socketIDToMessageManager.remove(socketManager.getSocketID());
            key.attach(null);
            key.cancel();
            key.channel().close();
        }
    }

    private void writeToSocket(SelectionKey key) throws IOException {
        SocketManager socketManager = (SocketManager) key.attachment();

        if (socketManager.sendMessages()) {
            // all messages have been sent
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    public void sendMessage(long socketID, Serializable message) {
        SocketManager socketManager = this.socketIDToMessageManager.get(socketID);
        try {
            if (!socketManager.sendMessage(message)) {
                // has still messages to send - add write selector
                SelectionKey key = socketManager.getSocketChannel().keyFor(selector);
                if ((key.interestOps() & SelectionKey.OP_WRITE) == 0) {
                    // write is not set yet
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
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
            PortListener portListener = (PortListener) key.attachment();

            SocketManager socketManager = new SocketManager(socketChannel, portListener.getMessageProcessor());
            initNewConnectedSocket(socketManager);

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
}
