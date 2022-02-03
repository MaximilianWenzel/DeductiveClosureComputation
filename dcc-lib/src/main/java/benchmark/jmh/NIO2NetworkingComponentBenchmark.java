package benchmark.jmh;

import com.google.common.base.Stopwatch;
import networking.NIO2NetworkingComponent;
import networking.ServerData;
import networking.messages.MessageEnvelope;
import nio2kryo.Edge;
import org.openjdk.jmh.runner.RunnerException;
import reactor.core.publisher.Flux;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static java.util.concurrent.TimeUnit.MILLISECONDS;


public class NIO2NetworkingComponentBenchmark {

    private static final int MESSAGE_COUNT = 10_000_000;
    private static final int BATCH_SIZE = 1;
    private SenderStub nio2SenderStub;
    private ReceiverStub nio2ReceiverStub;
    private Random rnd = new Random();

    public static void main(String[] args) throws RunnerException {
        runExperiment();
    }


    public static void runExperiment() {
        NIO2NetworkingComponentBenchmark benchmark = new NIO2NetworkingComponentBenchmark();
        benchmark.setUp();
        Stopwatch sw = Stopwatch.createStarted();
        BlockingQueue<Integer> result = new ArrayBlockingQueue<>(1);


        long destinationSocket = benchmark.nio2SenderStub.destinationSocket.getSocketID();
        Flux<MessageEnvelope> messages = Flux.range(1, MESSAGE_COUNT)
                .map(n -> new Edge(n, n))
                //.buffer(BATCH_SIZE)
                .map(obj -> new MessageEnvelope(destinationSocket, obj))
                .doOnNext(obj -> benchmark.nio2SenderStub.increaseHashSum(obj))
                .doOnComplete(() -> result.add(benchmark.nio2SenderStub.getHashSum()));

        benchmark.nio2ReceiverStub.setOnAllMessagesReceived(() -> result.add(benchmark.nio2ReceiverStub.getHashSum()));

        NIO2NetworkingComponent senderNetworkingComponent = benchmark.nio2SenderStub.getNetworkingComponent();

        benchmark.nio2SenderStub.threadPool.submit(() -> {
            messages.subscribe(senderNetworkingComponent.getNewSubscriberForMessagesToSend());
        });

        try {
            int hashSum1 = result.take();
            int hashSum2 = result.take();
            assert hashSum1 == hashSum2;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long objPerSec = MESSAGE_COUNT * 1000L / sw.elapsed(MILLISECONDS);
        benchmark.tearDown();
        System.out.println(objPerSec + " obj/s");
    }

    public void setUp() {
        nio2ReceiverStub = new ReceiverStub((long) Math.ceil((double) MESSAGE_COUNT / BATCH_SIZE));
        nio2SenderStub = new SenderStub(new ServerData("localhost", nio2ReceiverStub.getServerPort()));
    }

    public void tearDown() {
        System.out.println("Terminating networking components...");
        nio2SenderStub.terminate();
        nio2ReceiverStub.terminate();
    }


}
