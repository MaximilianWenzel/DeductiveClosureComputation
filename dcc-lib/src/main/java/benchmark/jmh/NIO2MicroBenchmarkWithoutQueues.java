package benchmark.jmh;

import com.google.common.base.Stopwatch;
import enums.NetworkingComponentType;
import networking.NIO2NetworkingComponent;
import networking.ServerData;
import networking.connectors.ConnectionModel;
import networking.io.MessageHandler;
import networking.io.SocketManager;
import networking.messages.MessageEnvelope;
import nio2kryo.Edge;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.*;

public class NIO2MicroBenchmarkWithoutQueues implements Runnable {

    private Random rnd = new Random();
    private BlockingQueue<Object> result = new ArrayBlockingQueue<>(1);

    private ReceiverStub receiverStub;

    private ExecutorService senderThreadPool;
    private NIO2NetworkingComponent sender;
    private SocketManager socketManager;
    private boolean lastMessageCouldBeSent = true;
    private MessageEnvelope lastMessageThatCouldNotBeSent;
    private Serializable nextMessageToBeSent;
    private Stopwatch sw = Stopwatch.createUnstarted();

    private int MESSAGE_COUNT = 10_000_000;
    private int sentMessages = 0;

    private ConnectionModel serverConnector;

    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            NIO2MicroBenchmarkWithoutQueues benchmark = new NIO2MicroBenchmarkWithoutQueues();
            benchmark.runExperiment();
            System.out.println(ConsoleUtils.getSeparator());
        }
    }

    public NIO2MicroBenchmarkWithoutQueues() {
        init();
    }

    private void init() {
        this.receiverStub = new ReceiverStub(NetworkingComponentType.ASYNC_NIO2);
        ServerData receiverData = new ServerData("localhost", receiverStub.getServerPort());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.senderThreadPool = Executors.newFixedThreadPool(1);
        MessageHandler messageHandler = (socketID, message) -> {};
        this.serverConnector = new ConnectionModel(
                receiverData, messageHandler) {
            @Override
            public void onConnectionEstablished(SocketManager socketManager) {
                NIO2MicroBenchmarkWithoutQueues.this.socketManager = socketManager;
                System.out.println("Starting benchmark.");
                sw = Stopwatch.createStarted();
                run();
            }

        };
        this.sender = new NIO2NetworkingComponent(
                senderThreadPool,
                messageEnvelope -> {
                    this.lastMessageThatCouldNotBeSent = messageEnvelope;
                    this.lastMessageCouldBeSent = false;
                },
                (socketID) -> {}
        );

        try {
            sender.connectToServer(serverConnector);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runExperiment() {
        try {
            result.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tearDown();
        System.out.println("Finished.");
    }

    public void tearDown() {
        receiverStub.terminate();
        sender.terminate();
        try {
            senderThreadPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

        this.result.add("done");
    }
}
