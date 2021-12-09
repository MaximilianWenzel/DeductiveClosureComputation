package benchmark.jmh;

import com.esotericsoftware.kryo.Kryo;
import networking.NetworkingComponent;
import networking.ServerData;
import networking.connectors.PortListener;
import networking.connectors.ServerConnector;
import networking.io.MessageProcessor;
import networking.io.SocketManager;
import networking.io.SocketManagerFactory;
import networking.messages.MessageEnvelope;
import networking.messages.MessageModel;
import networking.messages.SaturationAxiomsMessage;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import util.NetworkingUtils;
import util.SerializationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Queue;
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
public class NetworkCommunicationBenchmark {

    private NetworkingComponent sendingNetworkingComponent;
    private int serverPort;
    private NetworkingComponent receivingNetworkingComponent;

    private SocketManager destinationSocket;

    private LinkedBlockingQueue<Object> linkedBlockingQueue;
    private ArrayBlockingQueue<Object> arrayBlockingQueue;
    private BlockingQueue<Object> queue;

    private ArrayList axioms = new ArrayList(Arrays.asList(new Object[1]));

    public static void main(String[] args) throws RunnerException {
        SerializationUtils.kryo.register(TestObject.class);
        SerializationUtils.kryo.register(SaturationAxiomsMessage.class);
        SerializationUtils.kryo.register(MessageEnvelope.class);


        Options opt = new OptionsBuilder()
                .include(NetworkCommunicationBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Iteration)
    public void setUp() {
        linkedBlockingQueue = new LinkedBlockingQueue<>();
        arrayBlockingQueue = new ArrayBlockingQueue<>(1000);
        initializeReceivingNetworkingComponent(arrayBlockingQueue);

        queue = arrayBlockingQueue;
        initializeSendingNetworkingComponent();
    }

    @Benchmark
    public void sendingMessageModel() throws InterruptedException {
        this.axioms.set(0, new TestObject());
        SaturationAxiomsMessage axioms = new SaturationAxiomsMessage((long) (Math.random() * Long.MAX_VALUE), this.axioms);
        sendingNetworkingComponent.sendMessage(destinationSocket.getSocketID(), axioms);
        Object o = queue.take();
        assert o.getClass() != null;
    }

    @Benchmark
    public void sendingMessageModelInMessageEnvelop() throws InterruptedException {
        this.axioms.set(0, new TestObject());
        SaturationAxiomsMessage axioms = new SaturationAxiomsMessage((long) (Math.random() * Long.MAX_VALUE), this.axioms);
        MessageEnvelope envelope = new MessageEnvelope(0, axioms);
        sendingNetworkingComponent.sendMessage(destinationSocket.getSocketID(), envelope);
        Object o = queue.take();
        assert o.getClass() != null;
    }

    @Benchmark
    public void sendingMessageAsSingleInteger() throws InterruptedException {
        sendingNetworkingComponent.sendMessage(destinationSocket.getSocketID(), (long) (Math.random() * Long.MAX_VALUE));
        Object o = queue.take();
        assert o.getClass() != null;
    }

    @Benchmark
    public void sendingUnpackedAxiomAndMetaInfoEncodedAsIntegerIn2Messages() throws InterruptedException {
        sendingNetworkingComponent.sendMessage(destinationSocket.getSocketID(), new TestObject());
        sendingNetworkingComponent.sendMessage(destinationSocket.getSocketID(), (long) (Math.random() * Long.MAX_VALUE));
        Object o = queue.take();
        Object o2 = queue.take();
        assert o.getClass() != null;
        assert o2.getClass() != null;
    }


    @TearDown(Level.Iteration)
    public void tearDown() {
        System.out.println("Terminating networking components...");
        sendingNetworkingComponent.terminate();
        receivingNetworkingComponent.terminate();
    }

    private void initializeReceivingNetworkingComponent(Queue<Object> queue) {
        System.out.println("\n");
        this.serverPort = NetworkingUtils.getFreePort();
        PortListener portListener = new PortListener(serverPort) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                System.out.println("Client connected.");
            }
        };
        receivingNetworkingComponent = new NetworkingComponent(
                new SocketManagerFactory(),
                new MessageProcessor() {
                    @Override
                    public void process(MessageEnvelope message) {
                        assert message != null;
                        queue.add(message);
                    }
                },
                Collections.singletonList(portListener),
                Collections.emptyList()
        );
        receivingNetworkingComponent.startNIOThread();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void initializeSendingNetworkingComponent() {
        sendingNetworkingComponent = new NetworkingComponent(
                new SocketManagerFactory(),
                new MessageProcessor() {
                    @Override
                    public void process(MessageEnvelope message) {
                    }
                },
                Collections.emptyList(),
                Collections.emptyList()
        );
        sendingNetworkingComponent.startNIOThread();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        AtomicInteger connectionEstablished = new AtomicInteger(0);
        ServerData serverData = new ServerData("localhost", serverPort);
        ServerConnector serverConnector = new ServerConnector(serverData) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                destinationSocket = socketManager;
                connectionEstablished.getAndIncrement();
                System.out.println("Connection to server established.");
            }
        };
        try {
            System.out.println("Connecting to server...");
            sendingNetworkingComponent.connectToServer(serverConnector);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // wait until connection established
        while (connectionEstablished.get() == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
