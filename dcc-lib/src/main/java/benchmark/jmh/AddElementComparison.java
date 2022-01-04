package benchmark.jmh;

import data.DefaultToDo;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 2, time = 2000, timeUnit = MILLISECONDS)
@Measurement(iterations = 3, time = 2000, timeUnit = MILLISECONDS)
public class AddElementComparison {

    public static int SIZE = 10;
    UnifiedSet<TestObject> unifiedSet;
    BlockingQueue<TestObject> linkedBlockingQueue;
    BlockingQueue<TestObject> arrayBlockingQueue;
    TestObject[] array;
    AtomicInteger arrayPosition;

    public static void main(String[] args) throws RunnerException {
        System.out.println("Size: " + AddElementComparison.SIZE);
        Options opt = new OptionsBuilder()
                .include(AddElementComparison.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Iteration)
    public void setUp() {
        unifiedSet = new UnifiedSet<>(SIZE);
        linkedBlockingQueue = new LinkedBlockingQueue<>();
        arrayBlockingQueue = new DefaultToDo<>();
        array = new TestObject[SIZE];
        arrayPosition = new AtomicInteger(0);
    }

    @Benchmark
    public void addElementUnifiedSet() {
        unifiedSet.add(new TestObject());
        unifiedSet.clear();
    }

    @Benchmark
    public void addElementLinkedBlockingQueue() throws InterruptedException {
        linkedBlockingQueue.put(new TestObject());
        linkedBlockingQueue.clear();
    }

    @Benchmark
    public void addElementArrayBlockingQueue() {
        arrayBlockingQueue.add(new TestObject());
        arrayBlockingQueue.clear();
    }

    @Benchmark
    public void addElementArray() {
        array[arrayPosition.getAndIncrement()] = new TestObject();
        if (arrayPosition.get() / SIZE > 0) {
            arrayPosition.set(0);
        }
    }

}
