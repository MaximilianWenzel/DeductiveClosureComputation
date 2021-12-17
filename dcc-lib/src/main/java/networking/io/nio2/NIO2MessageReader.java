package networking.io.nio2;

import networking.io.MessageHandler;
import util.serialization.JavaSerializer;
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

    // TODO user defined buffer size
    protected ByteBuffer messageBuffer = ByteBuffer.allocate(((int) Math.pow(2, 20) * 2));
    private int bufferLimit = (int) (messageBuffer.capacity() * 0.1);

    protected boolean newMessageStarts = true;
    protected boolean endOfStreamReached = false;
    private Serializer serializer = new JavaSerializer();

    private MessageHandler messageHandler;
    private long socketID;

    private ReadCompletionHandler readCompletionHandler = new ReadCompletionHandler();
    private AtomicBoolean currentlyReading = new AtomicBoolean(false);

    public NIO2MessageReader(long socketID, AsynchronousSocketChannel socketChannel,
                             MessageHandler messageHandler) {
        this.socketChannel = socketChannel;
        this.socketID = socketID;
        this.messageHandler = messageHandler;
    }

    public void startReading() {
        currentlyReading.set(true);
        this.socketChannel.read(messageBuffer, null, readCompletionHandler);
    }

    public void deserializeMessagesFromBuffer() throws IOException, ClassNotFoundException, ExecutionException,
            InterruptedException {
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


    private class ReadCompletionHandler implements CompletionHandler<Integer, Object> {

        @Override
        public void completed(Integer result, Object attachment) {
            readBytes += result;
            try {
                deserializeMessagesFromBuffer();
            } catch (IOException | ClassNotFoundException | ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            if (readBytes == -1) {
                return;
            }
            socketChannel.read(messageBuffer, null, this);
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
        }
    }
}
