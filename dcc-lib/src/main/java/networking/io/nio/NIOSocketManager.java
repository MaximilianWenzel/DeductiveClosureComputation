package networking.io.nio;

import networking.io.MessageHandler;
import networking.io.SocketManager;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicLong;

public class NIOSocketManager implements SocketManager {

    public static final AtomicLong socketIDCounter = new AtomicLong(1);
    protected long socketID;
    protected SocketChannel socketChannel;
    protected NIOMessageReader messageReader;
    protected NIOMessageWriter messageWriter;

    public NIOSocketManager(SocketChannel socketChannel, MessageHandler messageHandler) {
        this.socketChannel = socketChannel;
        this.socketID = socketIDCounter.getAndIncrement();
        this.messageWriter = new NIOMessageWriter(socketChannel);
        this.messageReader = new NIOMessageReader(socketID, socketChannel, messageHandler);
    }

    public long getSocketID() {
        return socketID;
    }

    /**
     * Returns whether the complete message could be transmitted.
     */
    @Override
    public boolean sendMessage(Serializable message) throws IOException {
        return this.messageWriter.send(message);
    }

    /**
     * Returns whether all messages could be transmitted.
     */
    public boolean sendMessages() throws IOException {
        return this.messageWriter.sendMessages();
    }

    public void readMessages() throws IOException, ClassNotFoundException {
        messageReader.read();
    }

    public boolean endOfStreamReached() {
        return messageReader.isEndOfStreamReached();
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    @Override
    public boolean hasMessagesToSend() {
        return !this.messageWriter.isEmpty();
    }

    @Override
    public void close() throws IOException {
        this.socketChannel.close();
    }

    @Override
    public boolean hasMessagesToRead() {
        return this.messageReader.hasMessagesOrReadsCurrentlyMessage();
    }

}
