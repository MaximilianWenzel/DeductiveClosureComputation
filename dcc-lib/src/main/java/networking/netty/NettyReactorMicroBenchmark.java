package networking.netty;

import com.google.common.base.Stopwatch;
import io.netty.channel.*;
import networking.ServerData;
import networking.reactor.netty.echo.Edge;
import reactor.core.publisher.Flux;
import util.ConsoleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class NettyReactorMicroBenchmark {

    public static final int MESSAGE_COUNT = 10;
    public static final int NUM_INBOUND_HANDLERS = 3;
    public static final int NUM_OUTBOUND_HANDLERS = 3;

    public static final ServerData serverData = new ServerData("localhost", 8080);
    private static final AtomicReference<Stopwatch> sw = new AtomicReference<>(Stopwatch.createUnstarted());
    private static final Random rnd = new Random();

    public static void main(String[] args) {
        /*
        Scanner s = new Scanner(System.in);
        s.nextLine();
         */
/*
        for (int i = 0; i < 3; i++) {
        }

 */
        executeExperiment();
        System.out.println(ConsoleUtils.getSeparator());
    }

    public static void executeExperiment() {
        AtomicInteger serverHashSum = new AtomicInteger();
        AtomicInteger clientHashSum = new AtomicInteger();
        AtomicInteger receivedObjects = new AtomicInteger();

        List<ChannelInboundHandler> inboundHandlers = new ArrayList<>();
        for (int i = 0; i < NUM_INBOUND_HANDLERS; i++) {
            inboundHandlers.add(new SimpleInboundHandlerImpl(i));
        }
        List<ChannelOutboundHandler> outboundHandlers = new ArrayList<>();
        for (int i = 0; i < NUM_OUTBOUND_HANDLERS - 1; i++) {
            outboundHandlers.add(new ChannelOutboundHandlerImpl(i));
        }
        ChannelOutboundHandler lastOutboundBuffer = outboundHandlers.get(NUM_OUTBOUND_HANDLERS - 1);

        // server
        NettyReactorNetworkingComponent serverNetworkingComponent = new NettyReactorNetworkingComponent();

        NettyConnectionModel conServer = new NettyConnectionModel(
                serverData,
                (socket) -> {
                    System.out.println("Server: Connection established.");
                },
                (msg) -> {
                    serverHashSum.addAndGet(msg.hashCode());
                    receivedObjects.incrementAndGet();

                    if (receivedObjects.get() % 1_000_000 == 0) {
                        System.out.println("Received objects: " + receivedObjects.get());
                    }

                    if (receivedObjects.get() == MESSAGE_COUNT) {
                        sw.get().stop();

                        assert clientHashSum.get() == serverHashSum.get();

                        long timeInMS = sw.get().elapsed(TimeUnit.MILLISECONDS);
                        long objPerSec = MESSAGE_COUNT * 1000L / timeInMS;
                        System.out.println("Required time for " + MESSAGE_COUNT + " objects: " + timeInMS + "ms");
                        System.out.println(objPerSec + " obj/s");
                    }
                },
                Flux.empty(),
                inboundHandlers,
                outboundHandlers
        );
        serverNetworkingComponent.listenToPort(conServer);

        // client
        Flux<?> messages = Flux.range(1, MESSAGE_COUNT)
                .map(ignore -> new Edge(rnd.nextInt(10_000), rnd.nextInt(10_000)))
                .doOnNext(edge -> clientHashSum.addAndGet(clientHashSum.get()));

        NettyReactorNetworkingComponent clientNetworkingComponent = new NettyReactorNetworkingComponent();
        NettyConnectionModel conClient = new NettyConnectionModel(
                serverData,
                (socket) -> {
                    System.out.println("Client: Connection established.");
                    sw.set(Stopwatch.createStarted());

                },
                (msg) -> {
                    System.out.println("Client: " + msg);
                },
                messages,
                inboundHandlers,
                outboundHandlers
        );
        clientNetworkingComponent.connectToServer(conClient);

        clientNetworkingComponent.terminate();
        serverNetworkingComponent.terminate();

    }

    @ChannelHandler.Sharable
    private static class SimpleInboundHandlerImpl extends SimpleChannelInboundHandler {
        int id;
        public SimpleInboundHandlerImpl(int id) {
            this.id = id;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object o) {
            System.out.println("Inbound handler " + id + ": " + o);
            ctx.fireChannelRead(o);
        }
    }

    @ChannelHandler.Sharable
    private static class ChannelOutboundHandlerImpl extends ChannelOutboundHandlerAdapter {
        int id;

        public ChannelOutboundHandlerImpl(int id) {
            this.id = id;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            System.out.println("Outbound handler " + id);

            super.handlerAdded(ctx);
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            System.out.println("Removed: Outbound handler " + id);
            super.handlerRemoved(ctx);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            System.out.println("Outbound handler " + id + ": writing object: " + msg);
            ctx.write(msg, promise);
        }

    }

}
