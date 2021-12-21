package networking.io.nio;

import networking.io.MessageHandler;
import util.serialization.JavaSerializer;
import util.serialization.KryoSerializer;
import util.serialization.Serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NIOMessageReader {

    protected SocketChannel socketChannel;

    protected int messageSizeInBytes;
    protected int readBytes = 0;

    // TODO user defined buffer size
    private final int BUFFER_SIZE = 2 << 20;
    private final int MESSAGE_SIZE_BYTES = 4;
    protected ByteBuffer messageBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

    protected boolean newMessageStarts = true;
    protected boolean endOfStreamReached = false;
    private Serializer serializer = new KryoSerializer();

    private MessageHandler messageHandler;
    private long socketID;

    public NIOMessageReader(long socketID, SocketChannel socketChannel, MessageHandler messageHandler) {
        this.socketChannel = socketChannel;
        this.socketID = socketID;
        this.messageHandler = messageHandler;
    }

    public void read() throws IOException, ClassNotFoundException {
        readBytes += read(messageBuffer);

        this.messageBuffer.flip();
        while (readBytes > 0) {
            // read messages from buffer
            if (newMessageStarts) {
                if (messageBuffer.remaining() >= MESSAGE_SIZE_BYTES) {
                    // first read message size in bytes
                    onNewMessageSizeHasBeenRead();
                } else {
                    // more bytes required
                    break;
                }
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

    protected void onNewMessageSizeHasBeenRead() {
        newMessageStarts = false;
        messageSizeInBytes = messageBuffer.getInt();
        readBytes -= 4;
    }

    protected void onCompleteMessageHasBeenRead() throws IOException, ClassNotFoundException {
        messageHandler.process(socketID, getCompletedMessageAndClearBuffer());
    }

    protected int read(ByteBuffer byteBuffer) throws IOException {
        int bytesRead;
        int totalBytesRead = 0;

        do {
            bytesRead = this.socketChannel.read(byteBuffer);
            totalBytesRead += bytesRead;
        } while (bytesRead > 0);

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
