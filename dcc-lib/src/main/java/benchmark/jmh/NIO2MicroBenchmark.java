package benchmark.jmh;

import com.google.common.base.Stopwatch;
import nio2kryo.Edge;
import util.ConsoleUtils;
import util.NetworkingUtils;
import util.QueueFactory;
import util.serialization.KryoSerializer;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NIO2MicroBenchmark {

    public static final int MESSAGE_COUNT = 20_000_000;
    public static final Random rnd = new Random();
    public static final int BUFFER_SIZE = 1 << 20;

    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        s.nextLine();
        for (int i = 0; i < 3; i++) {
            executeExperiment();
            System.out.println(ConsoleUtils.getSeparator());
        }
    }

    public static void executeExperiment() {
        NIO2Server server = new NIO2Server();
        NIO2Client client = new NIO2Client(server.port);

        Stopwatch sw = Stopwatch.createStarted();
        client.start();

        int hashCodeServer = server.getResult();
        sw.stop();

        int hashCodeClient = client.hashSum;

        assert hashCodeClient == hashCodeServer;

        long timeInMS = sw.elapsed(TimeUnit.MILLISECONDS);
        long objPerSec = MESSAGE_COUNT * 1000L / timeInMS;
        System.out.println("Required time for " + MESSAGE_COUNT + " objects: " + timeInMS + "ms");
        System.out.println(objPerSec + " obj/s");

        server.terminate();
        client.terminate();
    }

    public static class NIO2Server {
        AsynchronousChannelGroup threadPool;
        AsynchronousServerSocketChannel server;
        int port;

        AsynchronousSocketChannel client;

        KryoSerializer kryoSerializer = new KryoSerializer();
        int BYTE_BUFFER_CAPACITY = BUFFER_SIZE;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BYTE_BUFFER_CAPACITY);

        int hashSum = 0;
        AtomicInteger counter = new AtomicInteger(0);

        BlockingQueue<Integer> result = new ArrayBlockingQueue<>(1);

        boolean messageSizeRead = false;
        int messageSize = 0;

        public NIO2Server() {
            port = NetworkingUtils.getFreePort();
            try {
                threadPool = AsynchronousChannelGroup.withFixedThreadPool(1, Thread::new);
                server = AsynchronousServerSocketChannel.open(threadPool);

                server.bind(new InetSocketAddress("localhost", port));
                server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
                    @Override
                    public void completed(AsynchronousSocketChannel result, Object attachment) {
                        client = result;
                        try {
                            System.out.println("Connection to client established: " + result.getLocalAddress());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        read();
                    }

                    @Override
                    public void failed(Throwable exc, Object attachment) {
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void read() {
            client.read(byteBuffer, null, new CompletionHandler<Integer, Object>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    deserializeFromBuffer();
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    throw new IllegalStateException();
                }
            });
        }

        private void deserializeFromBuffer() {
            byteBuffer.flip();
            while (true) {
                if (!messageSizeRead) {
                    if (byteBuffer.remaining() >= 4) {
                        messageSize = byteBuffer.getInt();
                        messageSizeRead = true;
                    } else {
                        break;
                    }
                } else {
                    if (byteBuffer.remaining() >= messageSize) {
                        Edge e = (Edge) kryoSerializer.deserializeFromByteBuffer(byteBuffer);
                        int objHash = e.hashCode();
                        hashSum += objHash;
                        counter.incrementAndGet();
                        messageSizeRead = false;
                    } else {
                        break;
                    }
                }
            }
            byteBuffer.compact();

            if (counter.get() < MESSAGE_COUNT) {
                read();
            } else {
                try {
                    result.put(hashSum);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public Integer getResult() {
            try {
                return result.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        }

        public void terminate() {
            try {
                threadPool.shutdownNow();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class NIO2Client {

        AtomicInteger counter = new AtomicInteger(0);
        int hashSum = 0;

        AsynchronousChannelGroup threadPool;
        AsynchronousSocketChannel server;

        BlockingQueue<Serializable> queue = QueueFactory.createSaturationToDo();
        BlockingQueue<Serializable> queue2 = QueueFactory.createSaturationToDo();

        KryoSerializer kryoSerializer = new KryoSerializer();
        int BYTE_BUFFER_CAPACITY = BUFFER_SIZE;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BYTE_BUFFER_CAPACITY);

        public NIO2Client(int serverPort) {
            try {
                threadPool = AsynchronousChannelGroup.withFixedThreadPool(1, Thread::new);
                InetSocketAddress hostAddress = new InetSocketAddress("localhost", serverPort);
                server = AsynchronousSocketChannel.open(threadPool);
                server.connect(hostAddress).get();
            } catch (IOException | ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void start() {
            try {
                serializeMessages();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void swapQueues() {
            //System.out.println("swap");
            BlockingQueue<Serializable> temp = queue;
            queue = queue2;
            queue2 = temp;
        }

        private void serializeMessages() throws InterruptedException {
            serializeObjsToBuffer();
            for (; counter.get() < MESSAGE_COUNT; counter.incrementAndGet()) {
                if (queue.remainingCapacity() == 0 && queue2.isEmpty()) {
                    swapQueues();
                }

                Edge obj = new Edge(rnd.nextInt(10_000), rnd.nextInt(10_000));
                hashSum += obj.hashCode();
                queue.put(obj);
            }
        }

        private void serializeObjsToBuffer() {
            while (!queue2.isEmpty() && byteBuffer.position() < 0.9 * BYTE_BUFFER_CAPACITY) {
                Serializable obj = queue2.poll();

                // reserve bytes for length
                byteBuffer.position(byteBuffer.position() + 4);

                // write object to buffer
                int start = byteBuffer.position();
                kryoSerializer.serializeToByteBuffer(obj, byteBuffer);
                int end = byteBuffer.position();

                // write length to buffer
                int numBytesObject = end - start;
                byteBuffer.position(byteBuffer.position() - numBytesObject - 4);
                byteBuffer.putInt(numBytesObject);

                // set position to end of object
                byteBuffer.position(end);

                if (queue2.isEmpty()) {
                    swapQueues();
                }
            }
            writeToSocket();

        }

        private void writeToSocket() {
            byteBuffer.flip();
            server.write(byteBuffer, null, new CompletionHandler<Integer, Object>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    byteBuffer.compact();

                    if (counter.get() < MESSAGE_COUNT || !queue2.isEmpty() || !queue.isEmpty()) {
                        serializeObjsToBuffer();
                    } else if (counter.get() == MESSAGE_COUNT && byteBuffer.position() > 0) {
                        writeToSocket();
                    }
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    throw new IllegalStateException();
                }
            });
        }

        public void terminate() {
            try {
                this.threadPool.shutdownNow();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
