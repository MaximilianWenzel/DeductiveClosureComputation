package networking.io;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class SocketManager {

    public static final AtomicLong socketIDCounter = new AtomicLong(1);

    private final long socketID;
    private final SocketChannel socketChannel;

    private final MessageReader messageReader;
    private final MessageWriter messageWriter;

    public SocketManager(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        this.socketID = socketIDCounter.getAndIncrement();
        this.messageWriter = new MessageWriter(socketChannel);
        this.messageReader = new MessageReader(socketChannel);
    }

    public long getSocketID() {
        return socketID;
    }

    public void enqueueMessageToSend(Object message) throws IOException {
        this.messageWriter.enqueue(message);
    }

    public void sendMessages() {
        try {
            this.messageWriter.write();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Queue<Object> readMessages() throws IOException {
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
