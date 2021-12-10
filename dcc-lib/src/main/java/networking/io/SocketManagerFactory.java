package networking.io;

import java.nio.channels.SocketChannel;

public class SocketManagerFactory {

    private double simulatedNetworkSpeedInMbps = -1d;

    public SocketManagerFactory(double simulatedNetworkSpeedInMbps) {
        this.simulatedNetworkSpeedInMbps = simulatedNetworkSpeedInMbps;
    }

    public SocketManagerFactory() {
    }

    public SocketManager createNewSocketManager(SocketChannel channel) {
        if (simulatedNetworkSpeedInMbps != -1) {
            return new SocketManagerForBenchmark(channel);
        } else {
            return new DefaultSocketManager(channel);
        }
    }
}
