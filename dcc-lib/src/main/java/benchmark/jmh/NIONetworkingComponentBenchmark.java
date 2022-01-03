package benchmark.jmh;

import benchmark.transitiveclosure.ToldReachability;
import data.DefaultToDo;
import enums.NetworkingComponentType;
import networking.ServerData;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 2, time = 2000, timeUnit = MILLISECONDS)
@Measurement(iterations = 3, time = 4000, timeUnit = MILLISECONDS)
public class NIONetworkingComponentBenchmark {

    private SenderStub nioSenderStub;
    private ReceiverStub nioReceiverStub;

    private BlockingQueue<Object> queue;
    private Random rnd = new Random();

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(NIONetworkingComponentBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();

/*
        Scanner s = new Scanner(System.in);
        s.nextLine();
        NIONetworkingComponentBenchmark b = new NIONetworkingComponentBenchmark();
        b.setUp();
        for (int i = 0; i < 100_000_000; i++) {
            b.sendObjectNIO();
        }
        b.tearDown();


 */
    }

    @Setup(Level.Trial)
    public void setUp() {
        queue = new DefaultToDo<>();
        nioReceiverStub = new ReceiverStub(queue, NetworkingComponentType.NIO);
        nioSenderStub = new SenderStub(new ServerData("localhost", nioReceiverStub.getServerPort()),
                NetworkingComponentType.NIO);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        System.out.println("Terminating networking components...");
        nioSenderStub.terminate();
        nioReceiverStub.terminate();
    }

    @Benchmark
    public void sendObjectNIO() {
        nioSenderStub.sendMessage(new ToldReachability(rnd.nextInt(10_000), rnd.nextInt(10_000)));
    }


}
