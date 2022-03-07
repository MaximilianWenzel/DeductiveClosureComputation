package benchmark.microbenchmark;

import nio2kryo.Edge;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 2, time = 2000, timeUnit = MILLISECONDS)
@Measurement(iterations = 3, time = 2000, timeUnit = MILLISECONDS)
public class FluxSinkBenchmark {

    private static final int NUM_SUBSCRIBERS = 8;
    private static final int REQUEST_RATE = Integer.MAX_VALUE;

    private Sinks.Many<Object> sinkRequestRateMax;
    private Sinks.Many<Object> sinkRequestRate_1_Element;
    private Sinks.Many<Object> sinkRequestRate_100_Elements;
    private Sinks.Many<Object> sinkRequestRate_1000_Elements;
    private Sinks.Many<Object> sinkRequestRate_1_000_000_Elements;

    private final FailureHandler failureHandler = new FailureHandler();
    private final Random rnd = new Random();
    private final Object obj = new Object();

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(FluxSinkBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Iteration)
    public void setUp() {
        sinkRequestRateMax = Sinks.many().multicast().onBackpressureBuffer(10, false);
        for (int i = 0; i < NUM_SUBSCRIBERS; i++) {
            sinkRequestRateMax.asFlux().subscribe(new TestSubscriber(Integer.MAX_VALUE));
        }

        sinkRequestRate_1_Element = Sinks.many().multicast().onBackpressureBuffer(10, false);
        for (int i = 0; i < NUM_SUBSCRIBERS; i++) {
            sinkRequestRate_1_Element.asFlux().subscribe(new TestSubscriber(1));
        }


        sinkRequestRate_100_Elements = Sinks.many().multicast().onBackpressureBuffer(10, false);
        for (int i = 0; i < NUM_SUBSCRIBERS; i++) {
            sinkRequestRate_100_Elements.asFlux().subscribe(new TestSubscriber(100));
        }

        sinkRequestRate_1000_Elements = Sinks.many().multicast().onBackpressureBuffer(10, false);
        for (int i = 0; i < NUM_SUBSCRIBERS; i++) {
            sinkRequestRate_1000_Elements.asFlux().subscribe(new TestSubscriber(1000));
        }

        sinkRequestRate_1_000_000_Elements = Sinks.many().multicast().onBackpressureBuffer(10, false);
        for (int i = 0; i < NUM_SUBSCRIBERS; i++) {
            sinkRequestRate_1_000_000_Elements.asFlux().subscribe(new TestSubscriber(1_000_000));
        }
    }


    @Benchmark
    public void sinkEmit_RequestRate_Max() {
        sinkRequestRateMax.emitNext(obj, failureHandler);
    }

    @Benchmark
    public void sinkEmit_RequestRate_1() {
        sinkRequestRate_1_Element.emitNext(obj, failureHandler);
    }

    @Benchmark
    public void sinkEmit_RequestRate_100() {
        sinkRequestRate_100_Elements.emitNext(obj, failureHandler);
    }

    @Benchmark
    public void sinkEmit_RequestRate_1000_Elements() {
        sinkRequestRate_1000_Elements.emitNext(obj, failureHandler);
    }

    @Benchmark
    public void sinkEmit_RequestRate_1_000_000_Elements() {
        sinkRequestRate_1_000_000_Elements.tryEmitNext(new Edge(rnd.nextInt(10_000), rnd.nextInt(10_000)));
    }


    public static class TestSubscriber implements Subscriber<Object> {

        Subscription subscription;
        int requestedElements = 0;
        int requestRate;

        public TestSubscriber(int requestRate) {
            this.requestRate = requestRate;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            this.subscription.request(requestRate);
        }

        @Override
        public void onNext(Object s) {
            assert s.hashCode() > 0;
            if (requestedElements % requestRate == 0) {
                subscription.request(requestRate);
            }
            requestedElements++;
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {
        }

    }

    private class FailureHandler implements Sinks.EmitFailureHandler {

        @Override
        public boolean onEmitFailure(SignalType signalType, Sinks.EmitResult emitResult) {
            return false;
        }
    }

}
