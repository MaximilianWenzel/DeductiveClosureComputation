package networking.io.nio2;

import com.esotericsoftware.kryo.io.ByteBufferInput;
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
    private final int BUFFER_SIZE = 512 << 10;
    private final int MESSAGE_SIZE_BYTES = 4;
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
        messageBuffer.flip();
        while (readBytes > 0) {
            // read messages from buffer
            if (newMessageStarts) {
                if (readBytes >= MESSAGE_SIZE_BYTES) {
                    // first read message size in bytes
                    onNewMessageSizeHasBeenRead();
                } else {
                    // more bytes required
                    break;
                }
            } else {
                if (readBytes >= messageSizeInBytes) {
                    // if message size is known
                    Object message = serializer.deserializeFromByteBuffer(messageBuffer);
                    prepareBufferForNewMessage();
                    messageHandler.process(socketID, message);
                } else {
                    break;
                }
            }
        }
        messageBuffer.compact();
    }

    protected void onNewMessageSizeHasBeenRead() {
        newMessageStarts = false;
        messageSizeInBytes = messageBuffer.getInt();
        readBytes -= MESSAGE_SIZE_BYTES;
    }

    protected void prepareBufferForNewMessage() {
        readBytes -= messageSizeInBytes;
        messageSizeInBytes = -1;
        newMessageStarts = true;
    }

    public boolean hasMessagesOrReadsCurrentlyMessage() {
        return readBytes > 0 || messageSizeInBytes != -1 || !newMessageStarts;
    }

    public boolean endOfStreamReached() {
        return endOfStreamReached;
    }

}
