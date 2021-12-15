package networking.io;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class SocketManager {

    public static final AtomicLong socketIDCounter = new AtomicLong(1);
    protected long socketID;
    protected SocketChannel socketChannel;
    protected MessageReader messageReader;
    protected MessageWriter messageWriter;

    public SocketManager(SocketChannel socketChannel, MessageProcessor messageProcessor) {
        this.socketChannel = socketChannel;
        this.socketID = socketIDCounter.getAndIncrement();
        this.messageWriter = new MessageWriter(socketChannel);
        this.messageReader = new MessageReader(socketID, socketChannel, messageProcessor);
    }

    public long getSocketID() {
        return socketID;
    }

    /**
     * Returns whether the complete message could be transmitted.
     */
    public boolean sendMessage(Serializable message) throws IOException {
        return this.messageWriter.send(message);
    }

    /**
     * Returns whether all messages could be transmitted.
     */
    public boolean sendMessages() throws IOException {
        return this.messageWriter.readFromBufferAndWriteToSocket();
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

    public boolean hasMessagesToSend() {
        return !this.messageWriter.isEmpty();
    }

    public void close() throws IOException {
        this.socketChannel.close();
    }

    public boolean hasMessagesToRead() {
        return this.messageReader.hasMessagesOrReadsCurrentlyMessage();
    }

}
