package benchmark.jmh;

import com.google.common.base.Stopwatch;
import enums.NetworkingComponentType;
import networking.NIO2NetworkingComponent;
import networking.ServerData;
import networking.connectors.ConnectionEstablishmentListener;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import networking.messages.MessageEnvelope;
import nio2kryo.Edge;

import java.io.Serializable;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NIO2MicroBenchmarkWithoutQueues implements Runnable {

    private Random rnd = new Random();

    private ReceiverStub receiverStub;

    private ExecutorService senderThreadPool;
    private NIO2NetworkingComponent sender;
    private SocketManager socketManager;
    private boolean lastMessageCouldBeSent = true;
    private MessageEnvelope lastMessageThatCouldNotBeSent;
    private Serializable nextMessageToBeSent;
    private Stopwatch sw = Stopwatch.createUnstarted();

    private int MESSAGE_COUNT = 100_000_000;
    private int sentMessages = 0;

    public static void main(String[] args) {
        NIO2MicroBenchmarkWithoutQueues benchmark = new NIO2MicroBenchmarkWithoutQueues();
        benchmark.runExperiment();
    }

    public NIO2MicroBenchmarkWithoutQueues() {
        init();
    }

    private void init() {
        ArrayBlockingQueue<Object> blockingQueue = new ArrayBlockingQueue<>(10);
        this.receiverStub = new ReceiverStub(blockingQueue, NetworkingComponentType.ASYNC_NIO2);
        ServerData receiverData = new ServerData("localhost", receiverStub.getServerPort());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.senderThreadPool = Executors.newFixedThreadPool(1);
        MessageHandler messageHandler = (socketID, message) -> {
        };
        ConnectionEstablishmentListener connectionEstablishmentListener = new ConnectionEstablishmentListener(
                receiverData, messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                NIO2MicroBenchmarkWithoutQueues.this.socketManager = socketManager;
                runExperiment();
            }

        };
        this.sender = new NIO2NetworkingComponent(
                Collections.emptyList(),
                Collections.singletonList(connectionEstablishmentListener),
                messageEnvelope -> {
                    NIO2MicroBenchmarkWithoutQueues.this.lastMessageThatCouldNotBeSent = messageEnvelope;
                    NIO2MicroBenchmarkWithoutQueues.this.lastMessageCouldBeSent = false;
                },
                senderThreadPool
        );
    }

    public void runExperiment() {
        sw.start();
        senderThreadPool.submit(NIO2MicroBenchmarkWithoutQueues.this);
        try {
            senderThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tearDown();
    }

    public void tearDown() {
        receiverStub.terminate();
    }

    @Override
    public void run() {
        for (; sentMessages < MESSAGE_COUNT; sentMessages++) {
            if (!lastMessageCouldBeSent) {
                lastMessageCouldBeSent = true;
                nextMessageToBeSent = lastMessageThatCouldNotBeSent.getMessage();
            } else {
                nextMessageToBeSent = new Edge(rnd.nextInt(10_000), rnd.nextInt(10_000));
            }
            socketManager.sendMessage(nextMessageToBeSent);

            if (!lastMessageCouldBeSent) {
                senderThreadPool.submit(this);
                return;
            }
        }
        sw.stop();
        long objPerSec = MESSAGE_COUNT * 1000L / sw.elapsed(TimeUnit.MILLISECONDS);
        System.out.println(objPerSec + " obj/s");

        senderThreadPool.shutdownNow();
    }
}
