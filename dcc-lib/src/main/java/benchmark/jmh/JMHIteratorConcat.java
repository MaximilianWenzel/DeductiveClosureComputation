package benchmark.jmh;

import com.google.common.collect.Iterators;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 2, time = 2000, timeUnit = MILLISECONDS)
@Measurement(iterations = 3, time = 2000, timeUnit = MILLISECONDS)
public class JMHIteratorConcat {

    final int NUM_ELEMENTS_TO_ADD = 10;
    Iterator<Object> concatenatedIterator = Collections.emptyIterator();
    IteratorChain<Object> iteratorChain = new IteratorChain<>();
    ArrayBlockingQueue<Object> arrayBlockingQueue = new ArrayBlockingQueue<>(NUM_ELEMENTS_TO_ADD);
    Stream<Object> stream;
    List<Object> objectList;
    int count = 0;

    int streamCounter = 0;
    Iterator<?> it = Stream.generate(() -> {
                return streamCounter++;
            }).map(n -> {
                List<Object> l = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    l.add(new Object());
                }
                return l;
            })
            .flatMap(Collection::stream)
            .iterator();

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JMHIteratorConcat.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Iteration)
    public void setUp() {
        for (int i = 0; i < NUM_ELEMENTS_TO_ADD; i++) {
            objectList = new ArrayList<>(NUM_ELEMENTS_TO_ADD);
            objectList.add(new Object());
        }
        stream = Stream.empty();
    }

    @Benchmark
    public void concatIterator() {
        concatenatedIterator = Iterators.concat(concatenatedIterator, objectList.iterator());
        if (count == NUM_ELEMENTS_TO_ADD) {
            concatenatedIterator = Collections.emptyIterator();
            count = 0;
        } else {
            count++;
        }
    }

    @Benchmark
    public void addToArrayBlockingQueue() {
        if (count == 10) {
            arrayBlockingQueue.clear();
            count = 0;
        }
        count++;
        arrayBlockingQueue.add(objectList.iterator());
    }

    @Benchmark
    public void streamConcat() {
        if (count == 10) {
            stream = Stream.empty();
            count = 0;
        }
        count++;
        stream = Stream.concat(stream, objectList.stream());
    }

    @Benchmark
    public void iteratorChain() {
        if (count == 10) {
            iteratorChain = new IteratorChain<>();
            count = 0;
        }
        count++;
        iteratorChain.addIterator(objectList.iterator());
    }


    @Benchmark
    public void streamIterator() {
        if (it.hasNext()) {
            Object o = it.next();
            if (o.hashCode() < 0) {
                throw new IllegalStateException();
            }
        }
    }

}

