package benchmark.jmh;

import networking.ServerData;
import networking.messages.MessageEnvelope;
import networking.messages.SaturationAxiomsMessage;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
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
public class NetworkCommunicationBenchmark {

    private SenderStub senderStub;
    private ReceiverStub receiverStub;

    private BlockingQueue<Object> queue;

    private ArrayList objects = new ArrayList(Arrays.asList(new Object[1]));

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(NetworkCommunicationBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Trial)
    public void setUp() {
        queue = new LinkedBlockingQueue<>();
        receiverStub = new ReceiverStub(queue);
        senderStub = new SenderStub(new ServerData("localhost", receiverStub.getServerPort()));
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        System.out.println("Terminating networking components...");
        senderStub.terminate();
        receiverStub.terminate();
    }

    @Benchmark
    public void sendString() throws InterruptedException {
        senderStub.sendMessage("Hello world!");
        Object o = queue.take();
        assert o.getClass() != null;
    }


    /*
    @Benchmark
    public void sendingMessageModel() throws InterruptedException {
        this.objects.set(0, new TestObject());
        SaturationAxiomsMessage<?, ?, ?> axioms = new SaturationAxiomsMessage<>((long) (Math.random() * Long.MAX_VALUE), this.objects);
        senderStub.sendMessage(axioms);
        Object o = queue.take();
        assert o.getClass() != null;
    }

    @Benchmark
    public void sendingMessageModelInMessageEnvelop() throws InterruptedException {
        this.objects.set(0, new TestObject());
        SaturationAxiomsMessage<?,?,?> axioms = new SaturationAxiomsMessage<>((long) (Math.random() * Long.MAX_VALUE), this.objects);
        MessageEnvelope envelope = new MessageEnvelope(0, axioms);
        senderStub.sendMessage(envelope);
        Object o = queue.take();
        assert o.getClass() != null;
    }

    @Benchmark
    public void sendingMessageAsSingleInteger() throws InterruptedException {
        senderStub.sendMessage((long) (Math.random() * Long.MAX_VALUE));
        Object o = queue.take();
        assert o.getClass() != null;
    }

    @Benchmark
    public void sendingUnpackedAxiomAndMetaInfoEncodedAsIntegerIn2Messages() throws InterruptedException {
        senderStub.sendMessage(new TestObject());
        senderStub.sendMessage((long) (Math.random() * Long.MAX_VALUE));
        Object o = queue.take();
        Object o2 = queue.take();
        assert o.getClass() != null;
        assert o2.getClass() != null;
    }

     */



}
