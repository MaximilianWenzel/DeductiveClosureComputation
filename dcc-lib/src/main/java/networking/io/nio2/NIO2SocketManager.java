package networking.io.nio2;

import networking.io.SocketManager;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class NIO2SocketManager implements SocketManager {

    public static final AtomicLong socketIDCounter = new AtomicLong(1);
    protected long socketID;
    protected AsynchronousSocketChannel socketChannel;
    protected NIO2MessageReader messageReader;
    protected NIO2MessageWriter messageWriter;

    public NIO2SocketManager(AsynchronousSocketChannel socketChannel,
                             Runnable onSocketCanReadNewMessages,
                             Consumer<Long> onSocketCanWriteMessages) {
        this.socketChannel = socketChannel;
        this.socketID = socketIDCounter.getAndIncrement();
        this.messageReader = new NIO2MessageReader(socketID, socketChannel, onSocketCanReadNewMessages);
        this.messageWriter = new NIO2MessageWriter(socketID, socketChannel, onSocketCanWriteMessages);
    }

    public void startReading() {
        messageReader.startReading();
    }

    public Object readNextMessage() {
        try {
            return messageReader.readNextMessage();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public boolean sendMessage(Object message) {
        return messageWriter.send(message);
    }

    @Override
    public boolean hasMessagesToSend() {
        return !messageWriter.isEmpty();
    }

    @Override
    public void close() throws IOException {
        this.messageReader.close();
        this.messageWriter.close();
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

    public boolean canWriteMessages() {
        return messageWriter.canWrite();
    }
}
