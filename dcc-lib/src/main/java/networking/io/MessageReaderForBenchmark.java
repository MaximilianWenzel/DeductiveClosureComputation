package networking.io;

import com.google.common.base.Stopwatch;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MessageReaderForBenchmark extends MessageReader {

    private Stopwatch stopwatch = Stopwatch.createStarted();
    private double simulatedNetworkSpeedInMBps;
    private double numberOfBytesPerMB = 1_000_000;
    private double numberOfNSPerS = 1E+9;
    private Duration requiredElapsedTimeForLatencySimulation;

    private List<Integer> receivedMessageSizesInBytes = new ArrayList<>();

    public MessageReaderForBenchmark(SocketChannel socketChannel, double simulatedNetworkSpeedInMbps) {
        super(socketChannel);
        this.simulatedNetworkSpeedInMBps = simulatedNetworkSpeedInMbps / 8;
    }

    @Override
    public void read() throws IOException, ClassNotFoundException {
        if (newMessageStarts) {
            // first read message size in bytes
            read(messageSizeBuffer);

            if (messageSizeBuffer.remaining() == 0) {
                onNewMessageSizeHasBeenRead();
            }
        }
        if (messageSizeInBytes != -1 && stopwatch.elapsed().compareTo(requiredElapsedTimeForLatencySimulation) >= 0) {
            // if message size is known and simulated latency has elapsed
            read(messageBuffer);
            if (messageBuffer.remaining() == 0) {
                onCompleteMessageHasBeenRead();
            }
        }
    }

    @Override
    protected void onNewMessageSizeHasBeenRead() {
        stopwatch = Stopwatch.createStarted();

        super.onNewMessageSizeHasBeenRead();

        this.receivedMessageSizesInBytes.add(messageSizeInBytes);

        double messageSizeInMB = messageSizeInBytes / numberOfBytesPerMB;
        double requiredTimeInNS = (messageSizeInMB / simulatedNetworkSpeedInMBps) * numberOfNSPerS;
        requiredElapsedTimeForLatencySimulation = Duration.ofNanos((long) requiredTimeInNS);
        //System.out.println("Required duration: " + requiredElapsedTimeForLatencySimulation.toMillis() + "ms");
        //System.out.println("Required duration: " + requiredElapsedTimeForLatencySimulation.toNanos() + "ns");
    }

    @Override
    protected void onCompleteMessageHasBeenRead() throws IOException, ClassNotFoundException {
        super.onCompleteMessageHasBeenRead();
    }
}
