package networking.io.nio2;

import data.DefaultToDo;
import util.serialization.KryoSerializer;
import util.serialization.Serializer;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class NIO2MessageWriter {

    private final AsynchronousSocketChannel socketChannel;
    // TODO user defined buffer size
    private final int BUFFER_SIZE = 512 << 10;
    private final int STOP_SERIALIZATION_TO_BUFFER_THRESHOLD = (int) (BUFFER_SIZE * 0.95);
    private final AtomicBoolean currentlyWritingMessage = new AtomicBoolean(false);
    private int MESSAGE_SIZE_BYTES = 4;
    private Serializer serializer = new KryoSerializer();
    private ByteBuffer messageBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private BlockingQueue<Serializable> messagesToSend = new DefaultToDo<>();

    public NIO2MessageWriter(AsynchronousSocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    /**
     * Returns whether all messages have been transmitted.
     */
    public boolean send(Serializable message) {
        messagesToSend.offer(message);

        writeMessagesIfRequired();
        return true;
    }

    private void writeMessagesIfRequired() {
        if (this.messagesToSend.isEmpty() && messageBuffer.position() == 0) {
            // no messages to send
            return;
        }

        if (currentlyWritingMessage.compareAndSet(false, true)) {
            try {
                serializeMessagesToBuffer();

            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
            readFromBufferAndWriteToSocket();
        }
    }

    public void serializeMessagesToBuffer() throws InterruptedException, IOException {
        while (!messagesToSend.isEmpty() && messageBuffer.position() < STOP_SERIALIZATION_TO_BUFFER_THRESHOLD) {
            Serializable message = messagesToSend.poll();

            // reserve bytes for length
            messageBuffer.position(messageBuffer.position() + MESSAGE_SIZE_BYTES);

            // write object to buffer
            int start = messageBuffer.position();
            serializer.serializeToByteBuffer(message, messageBuffer);
            int end = messageBuffer.position();

            // write length to buffer
            int numBytesObject = end - start;
            messageBuffer.position(messageBuffer.position() - numBytesObject - MESSAGE_SIZE_BYTES);
            messageBuffer.putInt(numBytesObject);

            // set position to end of object
            messageBuffer.position(end);
        }
    }

    public void readFromBufferAndWriteToSocket() {
        messageBuffer.flip();
        this.socketChannel.write(messageBuffer, null, new CompletionHandler<Integer, Object>() {
            @Override
            public void completed(Integer result, Object attachment) {
                messageBuffer.compact();
                currentlyWritingMessage.set(false);
                writeMessagesIfRequired();
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
            }
        });
    }

    public boolean isEmpty() {
        return messagesToSend.isEmpty();
    }

}
