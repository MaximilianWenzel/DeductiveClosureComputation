package benchmark.jmh;


import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 2, time = 10000, timeUnit = MILLISECONDS)
@Measurement(iterations = 3, time = 10000, timeUnit = MILLISECONDS)
public class InterProcessCommunicationBenchmark {

    private BlockingQueue<Object> receivedMessages;
    private MessageSender sender;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(InterProcessCommunicationBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Iteration)
    public void setUp() {
        receivedMessages = new LinkedBlockingQueue<>();
        this.sender = new MessageSender(receivedMessages);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        this.sender.terminate();
    }


    @Benchmark
    public void sendingObjects() throws InterruptedException {
        sender.sendMessage(new TestObject());
        Object o = receivedMessages.take();
        assert o.getClass() != null;
    }

    private class MessageSender implements Runnable {

        private BlockingQueue<Object> toSend = new LinkedBlockingQueue<>();
        private boolean running = true;
        private Thread t;
        private Queue<Object> receivedMessages;

        public MessageSender(Queue<Object> receivedMessages) {
            this.receivedMessages = receivedMessages;
            start();
        }

        @Override
        public void run() {
            try {
                while (running) {
                    receivedMessages.add(toSend.take());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void start() {
            t = new Thread(this);
            t.start();
        }

        public void sendMessage(Object m) {
            toSend.add(m);
        }

        public void terminate() {
            running = false;
            toSend.add(new Object());
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
