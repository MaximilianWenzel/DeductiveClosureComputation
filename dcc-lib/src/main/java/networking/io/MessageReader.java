package networking.io;

import util.SerializationUtils;

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

    // TODO user defined buffer size
    protected ByteBuffer messageBuffer = ByteBuffer.allocate(((int)Math.pow(2, 20) * 2));

    protected boolean newMessageStarts = true;
    protected boolean endOfStreamReached = false;

    public MessageReader(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void read() throws IOException, ClassNotFoundException {
        if (newMessageStarts) {
            // first read message size in bytes
            //read(messageSizeBuffer);

            /*
            if (messageSizeBuffer.remaining() == 0) {
                onNewMessageSizeHasBeenRead();
            }

             */
        }
        if (messageSizeInBytes != -1) {
            // if message size is known
            read(messageBuffer);
            if (fullMessageHasBeenRead()) {
                onCompleteMessageHasBeenRead();
            }
        }
    }

    protected void onNewMessageSizeHasBeenRead() {
        newMessageStarts = false;
        //messageSizeInBytes = messageSizeBuffer.getInt(0);
        //messageSizeBuffer.clear();
        messageBuffer.clear();
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

    protected Object getCompletedMessageAndClearBuffer() {
        if (fullMessageHasBeenRead()) {
            messageBuffer.flip();
            Object obj = SerializationUtils.kryoDeserializeFromByteBuffer(messageBuffer, 0, messageSizeInBytes);
            initBufferForNewMessage();
            return obj;
        } else {
            throw new IllegalStateException("Message has not read completely yet. Remaining bytes: " + messageBuffer.remaining());
        }
    }

    protected void initBufferForNewMessage() {
        messageBuffer.compact();
        if (messageBuffer.hasRemaining()) {
            this.messageSizeInBytes = this.messageBuffer.getInt();
            this.messageBuffer.clear();
            //this.messageSizeBuffer.clear();
            this.messageSizeInBytes = -1;
            this.newMessageStarts = true;
        }
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

    protected boolean fullMessageHasBeenRead() {
        return messageBuffer.remaining() < this.messageBuffer.capacity() - messageSizeInBytes;
    }
}
