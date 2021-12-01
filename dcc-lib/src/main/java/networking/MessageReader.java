package networking;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;

public class MessageReader {

    // TODO probably change queue implementation
    private final Queue<Object> completedMessages = new ArrayDeque<>();

    private SocketChannel socketChannel;

    private int messageSizeInBytes;
    private final ByteBuffer messageSizeBuffer = ByteBuffer.wrap(new byte[4]);

    private ByteBuffer messageBuffer;

    private boolean newMessageStarts = true;
    private boolean endOfStreamReached = false;

    public MessageReader(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void read() throws IOException {
        if (newMessageStarts) {
            // first read message size in bytes
            read(messageSizeBuffer);

            if (messageSizeBuffer.remaining() == 0) {
                newMessageStarts = false;
                messageSizeInBytes = messageSizeBuffer.getInt(0);
                messageSizeBuffer.clear();

                // TODO probably reuse previous byte buffer array
                messageBuffer = ByteBuffer.wrap(new byte[messageSizeInBytes]);
            }
        }
        if (messageSizeInBytes != -1) {
            // if message size is known
            read(messageBuffer);
            if (messageBuffer.remaining() == 0) {
                try {
                    this.completedMessages.add(getCompletedMessage());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected int read(ByteBuffer byteBuffer) throws IOException {
        int bytesRead = this.socketChannel.read(byteBuffer);
        int totalBytesRead = bytesRead;

        while (bytesRead > 0) {
            bytesRead = this.socketChannel.read(byteBuffer);
            totalBytesRead += bytesRead;
        }

        if (bytesRead == -1) {
            this.endOfStreamReached = true;
        }

        return totalBytesRead;
    }

    private Object getCompletedMessage() throws IOException, ClassNotFoundException {
        if (messageBuffer.remaining() == 0) {
            byte[] bytes = messageBuffer.array();
            ByteArrayInputStream byteArrayIS = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(byteArrayIS);
            Object obj = ois.readObject();
            clear();
            return obj;
        } else {
            throw new IllegalStateException("Message has not read completely yet. Remaining bytes: " + messageBuffer.remaining());
        }
    }

    private void clear() {
        this.messageBuffer.clear();
        this.messageSizeBuffer.clear();
        this.messageSizeInBytes = -1;
        this.newMessageStarts = true;
    }

    public boolean readsCurrentlyMessage() {
        return messageSizeInBytes != -1 && !newMessageStarts;
    }

    public Queue<Object> getReceivedMessages() {
        return this.completedMessages;
    }

    public boolean isEndOfStreamReached() {
        return endOfStreamReached;
    }
}
