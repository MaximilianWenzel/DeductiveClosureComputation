package networking.io.nio2;

import util.serialization.KryoSerializer;
import util.serialization.Serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class NIO2MessageReader {

    // TODO user defined buffer size
    private final int BUFFER_SIZE = 512 << 10;
    private final int STOP_SERIALIZATION_REMAINING_BYTES = (int) (BUFFER_SIZE * 0.1);
    private final int MESSAGE_SIZE_BYTES = 4;
    private final Serializer serializer = new KryoSerializer();
    protected AsynchronousSocketChannel socketChannel;
    protected int messageSizeInBytes;
    protected int readBytes;
    protected boolean newMessageStarts = true;
    protected boolean socketIsClosed = false;
    protected ByteBuffer messageBufferToWriteTo = ByteBuffer.allocateDirect(BUFFER_SIZE);
    protected ByteBuffer messageBufferToDeserializeFrom = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private long socketID;
    private Runnable onSocketCanReadNewMessages;

    public NIO2MessageReader(long socketID, AsynchronousSocketChannel socketChannel,
                             Runnable onSocketCanReadNewMessages) {
        this.socketChannel = socketChannel;
        this.socketID = socketID;
        this.onSocketCanReadNewMessages = onSocketCanReadNewMessages;
        init();
    }

    private void init() {
        // initialize buffer as if all messages have been read from it
        messageBufferToDeserializeFrom.compact();
    }

    public void startReading() {
        this.socketChannel.read(messageBufferToWriteTo, null, new CompletionHandler<>() {
            @Override
            public void completed(Integer result, Object attachment) {
                readBytes += result;

                // copy to 'deserialize'-buffer
                messageBufferToDeserializeFrom.compact();
                if (messageBufferToDeserializeFrom.remaining() > STOP_SERIALIZATION_REMAINING_BYTES) {
                    messageBufferToWriteTo.flip();

                    int maxNumberOfBytesToCopy = Math.min(messageBufferToDeserializeFrom.remaining(),
                            messageBufferToWriteTo.remaining());

                    // set limit to max number of bytes to write
                    int limitBefore = messageBufferToWriteTo.limit();
                    messageBufferToWriteTo.limit(messageBufferToWriteTo.position() + maxNumberOfBytesToCopy);

                    messageBufferToDeserializeFrom.put(messageBufferToWriteTo);

                    messageBufferToWriteTo.limit(limitBefore);
                    messageBufferToWriteTo.compact();
                }
                // prepare 'deserialize'-buffer for reading
                messageBufferToDeserializeFrom.flip();

                if (!socketIsClosed) {
                    if (readBytes > 0) {
                        onSocketCanReadNewMessages.run();
                    }
                    if (readBytes == -1) {
                        socketIsClosed = true;
                        return;
                    }
                    socketChannel.read(messageBufferToWriteTo, null, this);
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
            }
        });
    }

    public Object readNextMessage() throws IOException, ClassNotFoundException {
        if (readBytes > 0) {
            // read messages from buffer
            if (newMessageStarts) {
                if (readBytes >= MESSAGE_SIZE_BYTES) {
                    // first read message size in bytes
                    onNewMessageSizeHasBeenRead();
                } else {
                    // more bytes required
                    return null;
                }
            }
            if (!newMessageStarts) {
                if (readBytes >= messageSizeInBytes) {
                    // if message size is known
                    Object message = serializer.deserializeFromByteBuffer(messageBufferToDeserializeFrom);
                    prepareBufferForNewMessage();
                    return message;
                } else {
                    return null;
                }
            }
        }
        return null;
    }


    protected void onNewMessageSizeHasBeenRead() {
        newMessageStarts = false;
        messageSizeInBytes = messageBufferToDeserializeFrom.getInt();
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

    public void close() {
        this.socketIsClosed = true;
    }
}
