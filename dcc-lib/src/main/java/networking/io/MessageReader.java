package networking.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;

public class MessageReader {

    // TODO probably change queue implementation
    protected final Queue<Object> completedMessages = new ArrayDeque<>();

    protected SocketChannel socketChannel;

    protected int messageSizeInBytes;
    protected final ByteBuffer messageSizeBuffer = ByteBuffer.wrap(new byte[4]);

    protected ByteBuffer messageBuffer;

    protected boolean newMessageStarts = true;
    protected boolean endOfStreamReached = false;

    public MessageReader(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void read() throws IOException, ClassNotFoundException {
        if (newMessageStarts) {
            // first read message size in bytes
            read(messageSizeBuffer);

            if (messageSizeBuffer.remaining() == 0) {
                onNewMessageSizeHasBeenRead();
            }
        }
        if (messageSizeInBytes != -1) {
            // if message size is known
            read(messageBuffer);
            if (messageBuffer.remaining() == 0) {
                onCompleteMessageHasBeenRead();
            }
        }
    }

    protected void onNewMessageSizeHasBeenRead() {
        newMessageStarts = false;
        messageSizeInBytes = messageSizeBuffer.getInt(0);
        messageSizeBuffer.clear();

        // TODO probably reuse previous byte buffer array
        messageBuffer = ByteBuffer.wrap(new byte[messageSizeInBytes]);
    }

    protected void onCompleteMessageHasBeenRead() throws IOException, ClassNotFoundException {
        this.completedMessages.add(getCompletedMessageAndClearBuffer());
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

    protected Object getCompletedMessageAndClearBuffer() throws IOException, ClassNotFoundException {
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

    protected void clear() {
        this.messageBuffer.clear();
        this.messageSizeBuffer.clear();
        this.messageSizeInBytes = -1;
        this.newMessageStarts = true;
    }

    public boolean hasMessagesOrReadsCurrentlyMessage() {
        return !this.completedMessages.isEmpty() || messageSizeInBytes != -1 || !newMessageStarts;
    }

    public Queue<Object> getReceivedMessages() {
        return this.completedMessages;
    }

    public boolean isEndOfStreamReached() {
        return endOfStreamReached;
    }
}
