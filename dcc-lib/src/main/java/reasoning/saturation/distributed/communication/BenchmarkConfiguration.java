package reasoning.saturation.distributed.communication;

import networking.io.SocketManagerFactory;

public class BenchmarkConfiguration {


    private double simulatedNetworkSpeedInMbps;
    private SocketManagerFactory socketManagerFactory;

    public BenchmarkConfiguration(double simulatedNetworkSpeedInMbps) {
        this.simulatedNetworkSpeedInMbps = simulatedNetworkSpeedInMbps;
        this.socketManagerFactory = new SocketManagerFactory(simulatedNetworkSpeedInMbps);
    }

    public double getSimulatedNetworkSpeedInMbps() {
        return simulatedNetworkSpeedInMbps;
    }

    public SocketManagerFactory getSocketManagerFactory() {
        return socketManagerFactory;
    }
}
