package benchmark.jmh;

import benchmark.transitiveclosure.ToldReachability;
import com.google.common.base.Stopwatch;
import data.DefaultToDo;
import enums.NetworkingComponentType;
import javafx.scene.paint.Stop;
import networking.ServerData;
import nio2kryo.Edge;
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
@Measurement(iterations = 3, time = 5000, timeUnit = MILLISECONDS)
public class NIO2NetworkingComponentBenchmark {

    private SenderStub nio2SenderStub;
    private ReceiverStub nio2ReceiverStub;

    private BlockingQueue<Object> queue;
    private Random rnd = new Random();

    public static void main(String[] args) throws RunnerException {
        runJMH();
    }

    public static void runJMH() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(NIO2NetworkingComponentBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    public static void runExperiment() {
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        NIO2NetworkingComponentBenchmark benchmark = new NIO2NetworkingComponentBenchmark();
        benchmark.setUp();
        int MESSAGE_COUNT = 10_000_000;
        Stopwatch sw = Stopwatch.createStarted();
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            benchmark.sendObjectNIO2();
        }
        long objPerSec = MESSAGE_COUNT / sw.elapsed(MILLISECONDS) * 1000;
        benchmark.tearDown();
        System.out.println(objPerSec + " obj/s");
    }

    @Setup(Level.Trial)
    public void setUp() {
        queue = new DefaultToDo<>();
        nio2ReceiverStub = new ReceiverStub(queue, NetworkingComponentType.ASYNC_NIO);
        nio2SenderStub = new SenderStub(new ServerData("localhost", nio2ReceiverStub.getServerPort()),
                NetworkingComponentType.ASYNC_NIO);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        System.out.println("Terminating networking components...");
        nio2SenderStub.terminate();
        nio2ReceiverStub.terminate();
    }

    @Benchmark
    public void sendObjectNIO2() {
        nio2SenderStub.sendMessage(new Edge(rnd.nextInt(10_000), rnd.nextInt(10_000)));
    }


}
