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
    private int bufferLimit = (int) (messageBuffer.capacity() * 0.1);

    private int numBytesForLength = 4;
    private Serializer serializer = new JavaSerializer();
    private final AtomicBoolean currentlyWritingMessage = new AtomicBoolean(false);
    private BlockingQueue<Serializable> messagesToSend = new LinkedBlockingQueue<>();

    public NIO2MessageWriter(AsynchronousSocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }


    /**
     * Returns whether all messages have been transmitted.
     */
    public boolean send(Serializable message) {
        messagesToSend.add(message);

        writeMessagesIfRequired();
        return true;
    }

    private void writeMessagesIfRequired() {
        synchronized (currentlyWritingMessage) {
            if (!currentlyWritingMessage.get()) {
                currentlyWritingMessage.set(true);
                if (this.messagesToSend.isEmpty() && messageBuffer.position() == 0) {
                    // no messages to send
                    currentlyWritingMessage.set(false);
                    return;
                }
                try {
                    serializeMessagesToBuffer();

                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
                readFromBufferAndWriteToSocket();
            }
        }
    }

    public void serializeMessagesToBuffer() throws InterruptedException, IOException {
        while (!messagesToSend.isEmpty() && messageBuffer.remaining() > bufferLimit) {
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
        this.socketChannel.write(messageBuffer, null, writeCompletionHandler);
    }

    public boolean isEmpty() {
        return messagesToSend.isEmpty();
    }

    private class WriteCompletionHandler implements CompletionHandler<Integer, Object> {
        @Override
        public void completed(Integer result, Object attachment) {
            messageBuffer.compact();
            synchronized (currentlyWritingMessage) {
                currentlyWritingMessage.set(false);
            }
            writeMessagesIfRequired();
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
        }
    }


}
