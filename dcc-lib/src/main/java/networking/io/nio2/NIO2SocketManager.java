package networking.io.nio2;

import networking.io.MessageHandler;
import networking.io.SocketManager;
import networking.messages.MessageEnvelope;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class NIO2SocketManager implements SocketManager {

    public static final AtomicLong socketIDCounter = new AtomicLong(1);
    protected long socketID;
    protected AsynchronousSocketChannel socketChannel;
    protected NIO2MessageReader messageReader;
    protected NIO2MessageWriter messageWriter;
    protected MessageHandler messageHandler;

    public NIO2SocketManager(AsynchronousSocketChannel socketChannel,
                             MessageHandler messageHandler,
                             Consumer<Long> onSocketOutboundBufferHasSpace) {
        this.socketChannel = socketChannel;
        this.socketID = socketIDCounter.getAndIncrement();
        this.messageHandler = messageHandler;
        this.messageReader = new NIO2MessageReader(socketID, socketChannel, messageHandler);
        this.messageWriter = new NIO2MessageWriter(socketID, socketChannel, onSocketOutboundBufferHasSpace);
    }

    public void startReading() {
        messageReader.startReading();
    }


    @Override
    public boolean sendMessage(Serializable message) {
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
