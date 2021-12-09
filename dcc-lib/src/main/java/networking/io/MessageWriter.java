package networking.io;

import util.SerializationUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;

public class MessageWriter {
    // TODO probably change queue implementation
    private final Queue<Object> messagesToSend = new ArrayDeque<>();

    private final SocketChannel socketChannel;

    private final ByteBuffer messageSizeBuffer = ByteBuffer.wrap(new byte[4]);

    // TODO user defined buffer size
    private ByteBuffer messageBuffer = ByteBuffer.allocate(((int) Math.pow(2, 20) * 2));

    private Object currentMessage;

    private boolean newMessageStarts = true;

    public MessageWriter(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void enqueue(Object message) {
        messagesToSend.add(message);
    }

    public void write() throws IOException {
        if (newMessageStarts) {
            currentMessage = messagesToSend.poll();
            if (currentMessage == null) {
                return;
            }
            messageBuffer.clear();
            SerializationUtils.kryoSerializeToByteBuffer(currentMessage, messageBuffer);
            messageBuffer.flip();

            messageSizeBuffer.clear();
            messageSizeBuffer.putInt(0, messageBuffer.remaining());
            newMessageStarts = false;
        }

        if (messageSizeBuffer.hasRemaining()) {
            write(messageSizeBuffer);
        }

        if (!messageSizeBuffer.hasRemaining()) {
            write(messageBuffer);
            if (!messageBuffer.hasRemaining()) {
                // complete message sent
                currentMessage = null;
                newMessageStarts = true;
            }
        }
    }

    private int write(ByteBuffer byteBuffer) throws IOException {
        int bytesWritten = this.socketChannel.write(byteBuffer);
        int totalBytesWritten = bytesWritten;

        while (bytesWritten > 0 && byteBuffer.hasRemaining()) {
            bytesWritten = this.socketChannel.write(byteBuffer);
            totalBytesWritten += bytesWritten;
        }

        return totalBytesWritten;
    }

    public boolean isEmpty() {
        return this.messagesToSend.isEmpty() && this.currentMessage == null;
    }

}
