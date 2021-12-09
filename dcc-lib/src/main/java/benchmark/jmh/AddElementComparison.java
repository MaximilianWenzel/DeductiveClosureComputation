package benchmark.jmh;

import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.roaringbitmap.RoaringBitmap;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 2, time = 1000, timeUnit = MILLISECONDS)
@Measurement(iterations = 2, time = 1000, timeUnit = MILLISECONDS)
public class AddElementComparison {

    public static int size = 1_000_000;
    UnifiedSet<Integer> unifiedSet;
    RoaringBitmap rrBitmap;

    public static void main(String[] args) throws RunnerException {
        System.out.println("Size: " + AddElementComparison.size);
        Options opt = new OptionsBuilder()
                .include(AddElementComparison.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Iteration)
    public void setUp() {
        unifiedSet = new UnifiedSet<>();
        rrBitmap = new RoaringBitmap();
    }

    @Benchmark
    public void addElementUnifiedSet() {
        for (int i = 0; i < size; i++) {
            unifiedSet.add(i);
        }
        if (unifiedSet.isEmpty()) {
            throw new IllegalStateException();
        }
        System.out.println(unifiedSet.size());
    }

    @Benchmark
    public void addElementRoaringBitmaps() {
        for (int i = 0; i < size; i++) {
            rrBitmap.add(i);
        }
        if (rrBitmap.isEmpty()) {
            throw new IllegalStateException();
        }
        assert rrBitmap.rank(1000) != 0;
    }

}
