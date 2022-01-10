package networking.io.nio;

import util.QueueFactory;
import util.serialization.KryoSerializer;
import util.serialization.Serializer;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

public class NIOMessageWriter {
    private final SocketChannel socketChannel;
    private final int BUFFER_SIZE = 2 << 20;
    private final int STOP_SERIALIZATION_TO_BUFFER_THRESHOLD = (int) (BUFFER_SIZE * 0.9);
    // TODO user defined buffer size
    private BlockingQueue<Serializable> messagesToSend = QueueFactory.createSaturationToDo();
    private int numBytesForLength = 4;

    private Serializer serializer = new KryoSerializer();
    private ByteBuffer messageBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

    public NIOMessageWriter(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    /**
     * Returns whether all messages have been transmitted.
     */
    public boolean send(Serializable message) {
        try {
            messagesToSend.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean sendMessages() throws IOException {
        serializeMessagesToBuffer();
        return readFromBufferAndWriteToSocket() && messagesToSend.isEmpty();
    }

    public void serializeMessagesToBuffer() throws IOException {
        while (!messagesToSend.isEmpty() && messageBuffer.position() < STOP_SERIALIZATION_TO_BUFFER_THRESHOLD) {
            Serializable message = messagesToSend.poll();

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
    public boolean readFromBufferAndWriteToSocket() throws IOException {
        readFromBufferAndWriteToSocket(messageBuffer);
        return messageBuffer.position() == 0; // 'compact' operation sets position to beginning if buffer is empty
    }

    private int readFromBufferAndWriteToSocket(ByteBuffer byteBuffer) throws IOException {
        messageBuffer.flip();

        int totalBytesWritten = 0;
        int bytesWritten;
        do {
            bytesWritten = this.socketChannel.write(byteBuffer);
            totalBytesWritten += bytesWritten;
        } while (bytesWritten > 0);

        byteBuffer.compact();
        return totalBytesWritten;
    }

    public boolean isEmpty() {
        return messagesToSend.isEmpty() && messageBuffer.position() == 0;
    }

}
