package networking.netty;

import com.google.common.base.Stopwatch;
import io.netty.channel.*;
import networking.ServerData;
import networking.io.SocketManager;
import networking.reactor.netty.echo.Edge;
import reactor.core.publisher.Flux;
import util.ConsoleUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class NettyReactorMicroBenchmark {

    public static final int MESSAGE_COUNT = 100_000_000;

    public static final ServerData serverData = new ServerData("localhost", 8080);
    private static final AtomicReference<Stopwatch> sw = new AtomicReference<>(Stopwatch.createUnstarted());
    private static final Random rnd = new Random();
    private static BlockingQueue<Object> result = new ArrayBlockingQueue<>(1);

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

        // server
        NettyReactorNetworkingComponent serverNetworkingComponent = new NettyReactorNetworkingComponent();

        NettyConnectionModel conServer = new NettyConnectionModel(
                serverData,
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

                        result.add("finished");
                    }
                },
                (socket) -> {
                    System.out.println("Server: Connection established.");
                }
        );
        serverNetworkingComponent.listenToPort(conServer);

        // client
        Flux<?> messages = Flux.range(1, MESSAGE_COUNT)
                .map(ignore -> new Edge(rnd.nextInt(10_000), rnd.nextInt(10_000)))
                .doOnNext(edge -> clientHashSum.addAndGet(clientHashSum.get()));

        AtomicReference<SocketManager> socketManager = new AtomicReference<>();
        NettyReactorNetworkingComponent clientNetworkingComponent = new NettyReactorNetworkingComponent();
        NettyConnectionModel conClient = new NettyConnectionModel(
                serverData,
                (msg) -> {
                    System.out.println("Client: " + msg);
                },
                (socket) -> {
                    System.out.println("Client: Connection established.");
                    sw.set(Stopwatch.createStarted());
                    socketManager.set(socket);
                }
        );
        clientNetworkingComponent.connectToServer(conClient);

        try {
            System.out.println("Sending messages...");
            messages.subscribe(msg -> socketManager.get().sendMessage((Serializable) msg));
            result.take();
            System.out.println("Finished.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        clientNetworkingComponent.terminate();
        serverNetworkingComponent.terminate();

    }


}
