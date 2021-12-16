package networking.io.nio2;

import networking.io.MessageProcessor;
import networking.io.SocketManager;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicLong;

public class NIO2SocketManager implements SocketManager {

    public static final AtomicLong socketIDCounter = new AtomicLong(1);
    protected long socketID;
    protected AsynchronousSocketChannel socketChannel;
    protected NIO2MessageReader messageReader;
    protected NIO2MessageWriter messageWriter;

    public NIO2SocketManager(AsynchronousSocketChannel socketChannel, MessageProcessor messageProcessor) {
        this.socketChannel = socketChannel;
        this.socketID = socketIDCounter.getAndIncrement();
        this.messageReader = new NIO2MessageReader(socketID, socketChannel, messageProcessor);
        this.messageWriter = new NIO2MessageWriter(socketChannel);
    }

    @Override
    public boolean sendMessage(Serializable message) throws IOException {
        return messageWriter.send(message);
    }

    @Override
    public boolean hasMessagesToSend() {
        return !messageWriter.isEmpty();
    }

    @Override
    public void close() throws IOException {
        this.socketChannel.close();
    }

    @Override
    public boolean hasMessagesToRead() {
        return messageReader.hasMessagesOrReadsCurrentlyMessage();
    }

    @Override
    public long getSocketID() {
        return this.socketID;
    }
}
