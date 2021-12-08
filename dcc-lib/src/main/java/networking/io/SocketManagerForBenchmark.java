package networking.io;

import java.nio.channels.SocketChannel;

public class SocketManagerForBenchmark extends SocketManager {

    public SocketManagerForBenchmark(SocketChannel socketChannel, double simulatedNetworkSpeedInMbps) {
        this.socketChannel = socketChannel;
        this.socketID = socketIDCounter.getAndIncrement();
        this.messageWriter = new MessageWriter(socketChannel);
        this.messageReader = new MessageReaderForBenchmark(socketChannel, simulatedNetworkSpeedInMbps);
    }
}
