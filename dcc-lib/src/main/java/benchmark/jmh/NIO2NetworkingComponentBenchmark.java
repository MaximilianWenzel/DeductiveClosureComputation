package benchmark.jmh;

import benchmark.transitiveclosure.ToldReachability;
import enums.NetworkingComponentType;
import networking.ServerData;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 2, time = 2000, timeUnit = MILLISECONDS)
@Measurement(iterations = 3, time = 2000, timeUnit = MILLISECONDS)
public class NIO2NetworkingComponentBenchmark {

    private SenderStub nio2SenderStub;
    private ReceiverStub nio2ReceiverStub;

    private BlockingQueue<Object> queue;
    private Random rnd = new Random();

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(NIO2NetworkingComponentBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Trial)
    public void setUp() {
        queue = new LinkedBlockingQueue<>();
        nio2ReceiverStub = new ReceiverStub(queue, NetworkingComponentType.ASYNC_NIO);
        nio2SenderStub = new SenderStub(new ServerData("localhost", nio2ReceiverStub.getServerPort()), NetworkingComponentType.ASYNC_NIO);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        System.out.println("Terminating networking components...");
        nio2SenderStub.terminate();
        nio2ReceiverStub.terminate();
    }

    @Benchmark
    public void sendObjectNIO2() {
        nio2SenderStub.sendMessage(new ToldReachability(rnd.nextInt(10_000), rnd.nextInt(10_000)));
    }


}
