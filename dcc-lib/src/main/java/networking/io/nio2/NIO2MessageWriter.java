package networking.io.nio2;

import util.serialization.JavaSerializer;
import util.serialization.Serializer;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class NIO2MessageWriter {

    private final AsynchronousSocketChannel socketChannel;
    private WriteCompletionHandler writeCompletionHandler = new WriteCompletionHandler();
    // TODO user defined buffer size
    private ByteBuffer messageBuffer = ByteBuffer.allocate(((int) Math.pow(2, 20) * 2));

    private Object bufferLock = new Object();

    private int numBytesForLength = 4;
    private Serializer serializer = new JavaSerializer();
    private AtomicBoolean currentlyWritingMessage = new AtomicBoolean(false);
    private BlockingQueue<Serializable> messagesToSend = new LinkedBlockingQueue<>();

    public NIO2MessageWriter(AsynchronousSocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    /**
     * Returns whether all messages have been transmitted.
     */
    public boolean send(Serializable message) {
        messagesToSend.add(message);

        if (!currentlyWritingMessage.get()) {
            writeMessagesIfRequired();
        }
        return true;
    }

    private void writeMessagesIfRequired() {
        try {
            serializeMessagesToBuffer();
            if (messageBuffer.position() == 0) {
                // no messages to write
                return;
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        readFromBufferAndWriteToSocket();
    }

    public void serializeMessagesToBuffer() throws InterruptedException, IOException {
        while (!messagesToSend.isEmpty()) {
            Serializable message = messagesToSend.take();

            // reserve bytes for length
            messageBuffer.position(messageBuffer.position() + numBytesForLength);

            // write object to buffer
            int start = messageBuffer.position();
            serializer.serializeToByteBuffer(message, messageBuffer);
            int end = messageBuffer.position();

            // write length to buffer
            int numBytesObject = end - start;
            messageBuffer.position(messageBuffer.position() - numBytesObject - numBytesForLength);
            messageBuffer.putInt(numBytesObject);

            // set position to end of object
            messageBuffer.position(end);
        }
    }

    /**
     * Returns whether all messages have been written to the socket from the buffer.
     */
    public void readFromBufferAndWriteToSocket() {
        messageBuffer.flip();
        this.currentlyWritingMessage.set(true);
        this.socketChannel.write(messageBuffer, null, writeCompletionHandler);
    }

    public boolean isEmpty() {
        return messagesToSend.isEmpty();
    }

    private class WriteCompletionHandler implements CompletionHandler<Integer, Object> {
        @Override
        public void completed(Integer result, Object attachment) {
            messageBuffer.compact();
            currentlyWritingMessage.set(false);
            writeMessagesIfRequired();
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
        }
    }


}
