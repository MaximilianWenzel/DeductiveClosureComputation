package benchmark.microbenchmark;

import org.apache.commons.collections4.iterators.IteratorChain;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 2, time = 2000, timeUnit = MILLISECONDS)
@Measurement(iterations = 3, time = 2000, timeUnit = MILLISECONDS)
public class JMHIterationBenchmark {

    final int NUM_ELEMENTS_PER_ITERATOR = 1;
    final int NUM_CONCATENATED_ITERATORS = 1000;
    IteratorChain<Object> iteratorChain = new IteratorChain<>();
    ArrayBlockingQueue<Object> arrayBlockingQueue = new ArrayBlockingQueue<>(NUM_ELEMENTS_PER_ITERATOR * NUM_CONCATENATED_ITERATORS);
    List<Object> objectList;


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JMHIterationBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Iteration)
    public void setUp() {
        objectList = new ArrayList<>(NUM_ELEMENTS_PER_ITERATOR);
        for (int i = 0; i < NUM_ELEMENTS_PER_ITERATOR; i++) {
            objectList.add(new Object());
        }
    }

    @Benchmark
    public void iteratorChain() {
        iteratorChain = new IteratorChain<>();
        for (int i = 0; i < NUM_CONCATENATED_ITERATORS; i++) {
            iteratorChain.addIterator(objectList.iterator());
        }
        while (iteratorChain.hasNext()) {
            if (iteratorChain.next().hashCode() < 0) {
                throw new IllegalStateException();
            }
        }
    }

    @Benchmark
    public void arrayBlockingQueue() {
        arrayBlockingQueue.clear();
        for (int i = 0; i < NUM_CONCATENATED_ITERATORS; i++) {
            arrayBlockingQueue.addAll(objectList);
        }
        while (!arrayBlockingQueue.isEmpty()) {
            if (arrayBlockingQueue.remove().hashCode() < 0) {
                throw new IllegalStateException();
            }
        }
    }

}

