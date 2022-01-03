package benchmark.jmh;


import nio2kryo.Edge;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import util.serialization.JavaSerializer;
import util.serialization.KryoSerializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 2, time = 2000, timeUnit = MILLISECONDS)
@Measurement(iterations = 3, time = 2000, timeUnit = MILLISECONDS)
public class SerializationBenchmark {

    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2 << 10);
    KryoSerializer kryoSerializer;
    JavaSerializer javaSerializer;
    Random rnd = new Random();

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SerializationBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();

    }

    @Setup(Level.Iteration)
    public void setUp() {
        kryoSerializer = new KryoSerializer();
        javaSerializer = new JavaSerializer();

    }

    @TearDown(Level.Iteration)
    public void tearDown() {
    }


    public void javaSerialization() throws IOException {
        javaSerializer.serializeToByteBuffer(new Edge(rnd.nextInt(100000),
                rnd.nextInt(100000)), byteBuffer);
        assert byteBuffer.position() > 0;
        if (byteBuffer.position() > 0.9 * byteBuffer.capacity()) {
            byteBuffer.position(0);
        }
    }

    @Benchmark
    public void kryoSerialization() {
        kryoSerializer.serializeToByteBuffer(new Edge(rnd.nextInt(100000),
                rnd.nextInt(100000)), byteBuffer);
        assert byteBuffer.position() > 0;
        if (byteBuffer.position() > 0.9 * byteBuffer.capacity()) {
            byteBuffer.position(0);
        }
    }
}
