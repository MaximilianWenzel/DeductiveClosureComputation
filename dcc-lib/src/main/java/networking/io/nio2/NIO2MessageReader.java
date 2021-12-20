package networking.io.nio2;

import networking.io.MessageHandler;
import util.serialization.KryoSerializer;
import util.serialization.Serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NIO2MessageReader {

    protected AsynchronousSocketChannel socketChannel;

    protected int messageSizeInBytes;
    protected int readBytes;
    protected boolean newMessageStarts = true;
    protected boolean endOfStreamReached = false;
    // TODO user defined buffer size
    private final int BUFFER_SIZE = 2 << 20;
    protected ByteBuffer messageBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private final Serializer serializer = new KryoSerializer();

    private MessageHandler messageHandler;
    private long socketID;

    private AtomicBoolean currentlyReading = new AtomicBoolean(false);

    public NIO2MessageReader(long socketID, AsynchronousSocketChannel socketChannel,
                             MessageHandler messageHandler) {
        this.socketChannel = socketChannel;
        this.socketID = socketID;
        this.messageHandler = messageHandler;
    }

    public void startReading() {
        currentlyReading.set(true);
        this.socketChannel.read(messageBuffer, null, new CompletionHandler<Integer, Object>() {
            @Override
            public void completed(Integer result, Object attachment) {
                readBytes += result;
                try {
                    deserializeMessagesFromBuffer();
                } catch (IOException | ClassNotFoundException | ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
                if (readBytes == -1) {
                    endOfStreamReached = true;
                    return;
                }
                socketChannel.read(messageBuffer, null, this);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {

            }
        });
    }

    public void deserializeMessagesFromBuffer() throws IOException, ClassNotFoundException, ExecutionException,
            InterruptedException {
        this.messageBuffer.flip();
        while (readBytes > 0) {
            // read messages from buffer
            if (newMessageStarts && messageBuffer.remaining() >= 4) {
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

    protected void onNewMessageSizeHasBeenRead() {
        newMessageStarts = false;
        messageSizeInBytes = messageBuffer.getInt();
        readBytes -= 4;
    }

    protected void onCompleteMessageHasBeenRead() throws IOException, ClassNotFoundException {
        messageHandler.process(socketID, getCompletedMessageAndClearBuffer());
    }

    protected Object getCompletedMessageAndClearBuffer() throws IOException, ClassNotFoundException {
        Object obj = serializer.deserializeFromByteBuffer(messageBuffer);
        prepareBufferForNewMessage();
        return obj;
    }

    protected void prepareBufferForNewMessage() {
        readBytes -= messageSizeInBytes;
        messageSizeInBytes = -1;
        newMessageStarts = true;
    }

    public boolean hasMessagesOrReadsCurrentlyMessage() {
        return readBytes > 0 || messageSizeInBytes != -1 || !newMessageStarts;
    }

    public boolean isEndOfStreamReached() {
        return endOfStreamReached;
    }

    protected boolean moreCompletedMessagesInBuffer() {
        return readBytes >= messageSizeInBytes;
    }
}
