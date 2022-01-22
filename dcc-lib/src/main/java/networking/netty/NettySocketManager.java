package networking.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import networking.io.SocketManager;
import util.serialization.KryoSerializer;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class NettySocketManager implements SocketManager {

    private static final AtomicLong socketIDCounter = new AtomicLong(1);
    private final static int MESSAGE_SIZE_BYTES = 4;
    protected long socketID;
    private SocketChannel socketChannel;

    public NettySocketManager(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public boolean sendMessage(Serializable message) {
        socketChannel.write(message);
        return true;
    }

    @Override
    public boolean hasMessagesToSend() {
        return false;
    }

    @Override
    public void close() throws IOException {
        socketChannel.close();
    }

    @Override
    public boolean hasMessagesToRead() {
        return false;
    }

    @Override
    public long getSocketID() {
        return socketID;
    }

    public static class KryoDecoder extends ByteToMessageDecoder {

        private boolean newMessageStarts = true;
        private KryoSerializer serializer = new KryoSerializer();
        private int messageSizeInBytes;
        private int readBytes = 0;
        private int totalProcessedBytes = 0;
        private int initialPosition = 0;


        @Override
        protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out) {
            System.out.println("decoding... ");

            ByteBuffer buffer = byteBuf.nioBuffer();
            initialPosition = byteBuf.readerIndex();
            readBytes = byteBuf.readableBytes();
            totalProcessedBytes = 0;
            if (readBytes > 0) {
                while (readBytes > 0) {
                    // read messages from buffer
                    if (newMessageStarts) {
                        if (readBytes >= MESSAGE_SIZE_BYTES) {
                            // first read message size in bytes
                            newMessageStarts = false;
                            messageSizeInBytes = buffer.getInt();
                            readBytes -= MESSAGE_SIZE_BYTES;
                            totalProcessedBytes += MESSAGE_SIZE_BYTES;
                        } else {
                            // more bytes required
                            break;
                        }
                    } else {
                        if (readBytes >= messageSizeInBytes) {
                            // if message size is known
                            Object message = serializer.deserializeFromByteBuffer(buffer);
                            out.add(message);
                            readBytes -= messageSizeInBytes;
                            totalProcessedBytes += messageSizeInBytes;
                            newMessageStarts = true;
                        } else {
                            // more bytes required
                            break;
                        }
                    }
                }
                // set Netty ByteBuf index
                byteBuf.readerIndex(initialPosition + totalProcessedBytes);
            }

        }
    }

    public static class KryoEncoder extends MessageToByteEncoder<Object> {

        ByteBuffer buffer = ByteBuffer.allocateDirect(512 << 10);
        private KryoSerializer serializer = new KryoSerializer();

        @Override
        protected void encode(ChannelHandlerContext channelHandlerContext, Object serializable, ByteBuf byteBuf) {
            System.out.println("encoding... ");
            // reserve bytes for length
            buffer.position(buffer.position() + MESSAGE_SIZE_BYTES);

            // write object to buffer
            int start = buffer.position();
            serializer.serializeToByteBuffer((Serializable) serializable, buffer);
            int end = buffer.position();

            // write length to buffer
            int numBytesObject = end - start;
            buffer.position(
                    buffer.position() - numBytesObject - MESSAGE_SIZE_BYTES);
            buffer.putInt(numBytesObject);

            // set position to end of object
            buffer.position(end);
            buffer.flip();

            byteBuf.writeBytes(buffer);
            buffer.compact();

        }
    }
}
