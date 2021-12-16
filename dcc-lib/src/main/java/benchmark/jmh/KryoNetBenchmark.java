package benchmark.jmh;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import data.Closure;
import networking.messages.MessageEnvelope;
import networking.messages.SaturationAxiomsMessage;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import util.NetworkingUtils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
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
public class KryoNetBenchmark {

    Server server;
    Client client;
    int serverPort;
    private BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
    private ArrayList objects = new ArrayList(Arrays.asList(new Object[1]));

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(KryoNetBenchmark.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(opt).run();
    }

    private void registerKryoClasses(Kryo kryo) {
        kryo.register(TestObject.class);
        kryo.register(TestObjectWithFields.class);
        kryo.register(MessageEnvelope.class);
        kryo.register(SaturationAxiomsMessage.class);
        kryo.register(ArrayList.class);
        kryo.register(networking.messages.SaturationAxiomsMessage.class);
        kryo.register(Closure.class);
        kryo.register(Serializable.class);
    }

    @Setup(Level.Trial)
    public void setUp() {
        try {
            initServer();
            initClient();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        server.close();
        client.close();
    }

    private void initServer() throws IOException {
        server = new Server();
        registerKryoClasses(server.getKryo());
        serverPort = NetworkingUtils.getFreePort();

        server.start();
        server.bind(serverPort);
        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                queue.add(object);
            }
        });
    }

    private void initClient() throws IOException, InterruptedException {
        client = new Client();
        client.start();
        registerKryoClasses(client.getKryo());

        Thread clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client.connect(5000, "localhost", serverPort);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        clientThread.start();
        clientThread.join();
    }


    @Benchmark
    public void sendString() throws InterruptedException {
        client.sendTCP("Hello World!");
        Object o = queue.take();
        assert o.getClass() != null;
    }

    @Benchmark
    public void sendObject() throws InterruptedException {
        client.sendTCP(new TestObjectWithFields(10));
        Object o = queue.take();
        assert o.getClass() != null;
    }

}
