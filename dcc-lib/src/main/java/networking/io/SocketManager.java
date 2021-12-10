package networking.io;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public abstract class SocketManager {

    public static final AtomicLong socketIDCounter = new AtomicLong(1);
    protected long socketID;
    protected SocketChannel socketChannel;
    protected MessageReader messageReader;
    protected MessageWriter messageWriter;

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

    public Queue<Object> readMessages() throws IOException, ClassNotFoundException {
        messageReader.read();
        return this.messageReader.getReceivedMessages();
    }

    public Queue<Object> getReceivedMessages() {
        return this.messageReader.getReceivedMessages();
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
