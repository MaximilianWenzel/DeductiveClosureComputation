package networking.io.nio;

import util.serialization.JavaSerializer;
import util.serialization.Serializer;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NIOMessageWriter {
    private final SocketChannel socketChannel;

    // TODO user defined buffer size
    private ByteBuffer messageBuffer = ByteBuffer.allocate(((int) Math.pow(2, 20) * 2));

    private Object bufferLock = new Object();

    private int numBytesForLength = 4;
    private Serializer serializer = new JavaSerializer();

    public NIOMessageWriter(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    /**
     * Returns whether all messages have been transmitted.
     */
    public boolean send(Serializable message) throws IOException {
        // write message to buffer
        synchronized (bufferLock) {
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

        return readFromBufferAndWriteToSocket();
    }

    /**
     * Returns whether all messages have been written to the socket from the buffer.
     */
    public boolean readFromBufferAndWriteToSocket() throws IOException {
        synchronized (bufferLock) {
            readFromBufferAndWriteToSocket(messageBuffer);
            return messageBuffer.position() == 0; // 'compact' operation sets position to beginning if buffer is empty
        }
    }

    private int readFromBufferAndWriteToSocket(ByteBuffer byteBuffer) throws IOException {
        messageBuffer.flip();

        int totalBytesWritten = 0;
        int bytesWritten;
        do {
            bytesWritten = this.socketChannel.write(byteBuffer);
            totalBytesWritten += bytesWritten;
        } while (bytesWritten > 0 && byteBuffer.hasRemaining());

        byteBuffer.compact();
        return totalBytesWritten;
    }

    public boolean isEmpty() {
        synchronized (bufferLock) {
            return messageBuffer.position() == 0;
        }
    }

}
