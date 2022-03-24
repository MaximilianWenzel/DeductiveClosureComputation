package networking.io.nio2;

import util.serialization.KryoSerializer;
import util.serialization.Serializer;

import java.io.IOException;
import java.io.Serializable;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * This class provides methods in order to serialize messages to the appropriate socket outbound buffer and initiates by itself an
 * asynchronous, non-blocking write operation using the NIO.2 Asynchronous Socket Channel API.
 */
public class NIO2MessageWriter {

    private final AsynchronousSocketChannel socketChannel;
    private final int BUFFER_SIZE = 512 << 10;
    private final int STOP_SERIALIZATION_REMAINING_BYTES = (int) (BUFFER_SIZE * 0.1);
    private final AtomicBoolean currentlyWritingMessage = new AtomicBoolean(false);
    private final int MESSAGE_SIZE_BYTES = 4;
    private final Serializer serializer = new KryoSerializer();
    private final long socketID;
    private final Consumer<Long> onSocketOutboundBufferHasSpace;
    private ByteBuffer messageBufferToReadFrom = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private ByteBuffer messageBufferToWriteTo = ByteBuffer.allocateDirect(BUFFER_SIZE);

    public NIO2MessageWriter(long socketID, AsynchronousSocketChannel socketChannel,
                             Consumer<Long> onSocketOutboundBufferHasSpace) {
        this.socketChannel = socketChannel;
        this.socketID = socketID;
        this.onSocketOutboundBufferHasSpace = onSocketOutboundBufferHasSpace;
    }

    /**
     * Returns whether the message could be transmitted.
     */
    public boolean send(Serializable message) {
        try {
            boolean messageCouldBeSent;
            if (messageBufferToWriteTo.remaining() > STOP_SERIALIZATION_REMAINING_BYTES) {
                serializeMessageToBuffer(message);
                messageCouldBeSent = true;
            } else {
                messageCouldBeSent = false;
            }
            writeMessagesIfRequired();
            return messageCouldBeSent;
        } catch (IOException e) {
            throw new BufferOverflowException();
        }
    }

    private void writeMessagesIfRequired() {
        if (messageBufferToWriteTo.remaining() > STOP_SERIALIZATION_REMAINING_BYTES) {
            onSocketOutboundBufferHasSpace.accept(this.socketID);
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

    private void serializeMessageToBuffer(Serializable message) throws IOException {
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
            } else {
                throw new BufferOverflowException();
            }
        }
    }

    public void readFromBufferAndWriteToSocket() {
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
                exc.printStackTrace();
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

}
