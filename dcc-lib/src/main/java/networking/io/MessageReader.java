package networking.io;

import util.serialization.JavaSerializer;
import util.serialization.Serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;

public class MessageReader {

    // TODO probably change queue implementation
    protected final Queue<Object> completedMessages = new ArrayDeque<>();

    protected SocketChannel socketChannel;

    protected int messageSizeInBytes;
    protected int readBytes = 0;

    // TODO user defined buffer size
    protected ByteBuffer messageBuffer = ByteBuffer.allocate(((int) Math.pow(2, 20) * 2));

    protected boolean newMessageStarts = true;
    protected boolean endOfStreamReached = false;
    private Serializer serializer = new JavaSerializer();

    public MessageReader(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void read() throws IOException, ClassNotFoundException {
        readBytes += read(messageBuffer);

        this.messageBuffer.flip();
        while (readBytes > 0) {
            // read messages from buffer
            if (newMessageStarts && messageBuffer.hasRemaining()) {
                // first read message size in bytes
                onNewMessageSizeHasBeenRead();
            }
            if (messageSizeInBytes != -1 && moreCompletedMessagesInBuffer()) {
                // if message size is known
                onCompleteMessageHasBeenRead();
            }

            if (!moreCompletedMessagesInBuffer()) {
                break;
            }
        }
        messageBuffer.compact();
    }

    protected void onBytesHaveBeenRead() {

    }

    protected void onNewMessageSizeHasBeenRead() {
        newMessageStarts = false;
        messageSizeInBytes = messageBuffer.getInt();
        readBytes -= 4;
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
        Object obj = serializer.deserializeFromByteBuffer(messageBuffer);
        initBufferForNewMessage();
        return obj;
    }

    protected void initBufferForNewMessage() {
        readBytes -= messageSizeInBytes;
        messageSizeInBytes = -1;
        newMessageStarts = true;
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

    protected boolean moreCompletedMessagesInBuffer() {
        return readBytes >= messageSizeInBytes;
    }
}
