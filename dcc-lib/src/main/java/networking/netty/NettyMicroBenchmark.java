package networking.netty;

import com.google.common.base.Stopwatch;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import networking.ServerData;
import nio2kryo.Edge;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.netty.NettyOutbound;
import util.ConsoleUtils;

import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class NettyMicroBenchmark {

    public static final int MESSAGE_COUNT = 2;
    public static final BlockingQueue<Integer> hashSumResults = new ArrayBlockingQueue<>(2);
    private static final Random rnd = new Random();


    public static void main(String[] args) {
        /*
        Scanner s = new Scanner(System.in);
        s.nextLine();

         */
        for (int i = 0; i < 3; i++) {
            executeExperiment();
            System.out.println(ConsoleUtils.getSeparator());
        }
    }

    public static void executeExperiment() {


        // server
        NettyNetworkingComponent serverNetworkingComponent = new NettyNetworkingComponent(1);
        ServerData serverData = new ServerData("localhost", 8080);
        NettyServer server = new NettyServer();
        serverNetworkingComponent.listenToPort(
                serverData,
                (NettySocketManager) -> {
                },
                Collections.singletonList(server),
                Collections.emptyList()
        );

        // client
        NettyNetworkingComponent clientNetworkingComponent = new NettyNetworkingComponent(1);
        SimpleClient client = new SimpleClient(hashSumResults);
        clientNetworkingComponent.connectToServer(
                new ServerData("localhost", 8080),
                (NettySocketManager) -> {
                },
                Collections.singletonList(client),
                Collections.emptyList()
        );

        Stopwatch sw = Stopwatch.createStarted();

        try {
            System.out.println("Waiting for hash sums...");
            int hashSum1 = hashSumResults.take();
            int hashSum2 = hashSumResults.take();
            assert hashSum1 == hashSum2;
            System.out.println("Hash sums received.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sw.stop();

        clientNetworkingComponent.terminate();
        serverNetworkingComponent.terminate();

        long timeInMS = sw.elapsed(TimeUnit.MILLISECONDS);
        long objPerSec = MESSAGE_COUNT * 1000L / timeInMS;
        System.out.println("Required time for " + MESSAGE_COUNT + " objects: " + timeInMS + "ms");
        System.out.println(objPerSec + " obj/s");
    }

    public static class SimpleClient extends ChannelInboundHandlerAdapter implements Subscriber<Edge> {

        Flux<Edge> flux = Flux.fromStream(Stream.generate(() -> new Edge(rnd.nextInt(10_000), rnd.nextInt(10_000)))
                .limit(MESSAGE_COUNT));
        private BlockingQueue<Integer> result;
        private int sentMessages = 0;
        private int hashSum = 0;
        private ChannelHandlerContext ctx;
        private Subscription subscription;

        public SimpleClient(BlockingQueue<Integer> result) {
            this.result = result;

        }

        @Override
        public void channelActive(ChannelHandlerContext channelHandlerContext) {
            System.out.println("Client active");
            this.ctx = channelHandlerContext;
            flux.subscribeWith(this);
        }


        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(Edge edge) {
            Object obj = new Edge(rnd.nextInt(10_000), rnd.nextInt(10_000));
            ctx.write(obj);
            hashSum += obj.hashCode();
            sentMessages++;
            //System.out.println("Client: Sending message " + sentMessages);
            if (!ctx.channel().isWritable() || sentMessages == MESSAGE_COUNT) {
                ctx.flush();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            System.err.println("Error: " + throwable.getMessage());
        }

        @Override
        public void onComplete() {
            System.out.println("Client: All messages have been sent.");
            result.add(hashSum);
        }

    }


    public static class NettyServer extends ChannelInboundHandlerAdapter {


        int receivedMessages = 0;
        int hashSum = 0;
        private BlockingQueue<Integer> result = NettyMicroBenchmark.hashSumResults;

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            System.out.println("Server active");
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            receivedMessages++;
            if (receivedMessages % 1_000_000 == 0) {
                System.out.println("Received message: " + msg + ", (" + receivedMessages + "/" + MESSAGE_COUNT + ")");
            }
            if (receivedMessages < NettyMicroBenchmark.MESSAGE_COUNT) {
                hashSum += msg.hashCode();
            } else {
                result.add(hashSum);
            }
        }

    }
}
