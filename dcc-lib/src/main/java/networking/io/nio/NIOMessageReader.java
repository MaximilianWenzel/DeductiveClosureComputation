package networking.io.nio;

import networking.io.MessageProcessor;
import util.serialization.JavaSerializer;
import util.serialization.Serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NIOMessageReader {

    protected SocketChannel socketChannel;

    protected int messageSizeInBytes;
    protected int readBytes = 0;

    // TODO user defined buffer size
    protected ByteBuffer messageBuffer = ByteBuffer.allocate(((int) Math.pow(2, 20) * 2));

    protected boolean newMessageStarts = true;
    protected boolean endOfStreamReached = false;
    private Serializer serializer = new JavaSerializer();

    private MessageProcessor messageProcessor;
    private long socketID;

    public NIOMessageReader(long socketID, SocketChannel socketChannel, MessageProcessor messageProcessor) {
        this.socketChannel = socketChannel;
        this.socketID = socketID;
        this.messageProcessor = messageProcessor;
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
        messageProcessor.process(socketID, getCompletedMessageAndClearBuffer());
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
        return messageSizeInBytes != -1 || !newMessageStarts;
    }

    public boolean isEndOfStreamReached() {
        return endOfStreamReached;
    }

    protected boolean moreCompletedMessagesInBuffer() {
        return readBytes >= messageSizeInBytes;
    }
}
