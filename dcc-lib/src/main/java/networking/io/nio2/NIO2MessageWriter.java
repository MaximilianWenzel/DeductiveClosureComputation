package networking.io.nio2;

import util.serialization.KryoSerializer;
import util.serialization.Serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class NIO2MessageWriter {

    private final AsynchronousSocketChannel socketChannel;
    // TODO user defined buffer size
    private final int BUFFER_SIZE = 512 << 10;
    private final int STOP_SERIALIZATION_REMAINING_BYTES = (int) (BUFFER_SIZE * 0.1);
    private final AtomicBoolean currentlyWritingMessage = new AtomicBoolean(false);
    private int MESSAGE_SIZE_BYTES = 4;
    private Serializer serializer = new KryoSerializer();
    private Consumer<Long> onSocketCanWriteMessages;
    private long socketID;

    private ByteBuffer messageBufferToReadFrom = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private ByteBuffer messageBufferToWriteTo = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private boolean socketIsClosed = false;


    public NIO2MessageWriter(long socketID, AsynchronousSocketChannel socketChannel, Consumer<Long> onSocketCanWriteMessages) {
        this.socketID = socketID;
        this.socketChannel = socketChannel;
        this.onSocketCanWriteMessages = onSocketCanWriteMessages;
    }

    /**
     * Returns whether message could be transmitted.
     */
    public boolean send(Object message) {
        try {
            serializeMessageToBuffer(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        writeMessagesIfRequired();
        return true;
    }

    private void writeMessagesIfRequired() {
        if (canWrite()) {
            onSocketCanWriteMessages.accept(this.socketID);
        }

        if (messageBufferToWriteTo.position() == 0 && messageBufferToReadFrom.position() == 0) {
            // no messages to send
            return;
        }

        if (currentlyWritingMessage.compareAndSet(false, true)) {
            if (messageBufferToWriteTo.position() > 0 && messageBufferToReadFrom.position() == 0) {
                switchBuffers();
            }
            readFromBufferAndWriteToSocket();
        }
    }

    private boolean serializeMessageToBuffer(Object message) throws IOException {
        synchronized (messageBufferToWriteTo) {
            if (messageBufferToWriteTo.remaining() > STOP_SERIALIZATION_REMAINING_BYTES) {

                // reserve bytes for length
                messageBufferToWriteTo.position(messageBufferToWriteTo.position() + MESSAGE_SIZE_BYTES);

                // write object to buffer
                int start = messageBufferToWriteTo.position();
                serializer.serializeToByteBuffer(message, messageBufferToWriteTo);
                int end = messageBufferToWriteTo.position();

                // write length to buffer
                int numBytesObject = end - start;
                messageBufferToWriteTo.position(
                        messageBufferToWriteTo.position() - numBytesObject - MESSAGE_SIZE_BYTES);
                messageBufferToWriteTo.putInt(numBytesObject);

                // set position to end of object
                messageBufferToWriteTo.position(end);

                // return whether buffer is full now
                return messageBufferToWriteTo.remaining() < STOP_SERIALIZATION_REMAINING_BYTES;
            } else {
                throw new IllegalStateException("Cannot serialize message to buffer because it is full.");
            }
        }
    }

    public boolean canWrite() {
        synchronized (messageBufferToWriteTo) {
            return messageBufferToWriteTo.remaining() > STOP_SERIALIZATION_REMAINING_BYTES;
        }
    }

    public void readFromBufferAndWriteToSocket() {
        if (socketIsClosed) {
            return;
        }

        messageBufferToReadFrom.flip();
        this.socketChannel.write(messageBufferToReadFrom, null, new CompletionHandler<>() {
            @Override
            public void completed(Integer result, Object attachment) {
                messageBufferToReadFrom.compact();
                currentlyWritingMessage.set(false);
                writeMessagesIfRequired();
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
            }
        });

    }

    private void switchBuffers() {
        synchronized (messageBufferToReadFrom) {
            synchronized (messageBufferToWriteTo) {
                // switch buffers
                ByteBuffer temp = messageBufferToReadFrom;
                messageBufferToReadFrom = messageBufferToWriteTo;
                messageBufferToWriteTo = temp;
            }
        }
    }

    public boolean isEmpty() {
        return messageBufferToWriteTo.position() == 0 && messageBufferToReadFrom.position() == 0;
    }

    public void close() {
        this.socketIsClosed = true;
    }
}
