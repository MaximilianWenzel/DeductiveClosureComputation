package networking.io;

import java.nio.channels.SocketChannel;

public class DefaultSocketManager extends SocketManager {

    public DefaultSocketManager(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        this.socketID = socketIDCounter.getAndIncrement();
        this.messageWriter = new MessageWriter(socketChannel);
        this.messageReader = new MessageReader(socketChannel);
    }
}
