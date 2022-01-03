package benchmark.jmh;

import benchmark.transitiveclosure.ToldReachability;
import com.google.common.base.Stopwatch;
import nio2kryo.Edge;
import util.ConsoleUtils;
import util.NetworkingUtils;
import util.serialization.KryoSerializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class NIO2MicroBenchmark {

    public static final int MESSAGE_COUNT = 100_000_000;
    public static final Random rnd = new Random();

    public static void main(String[] args) {
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
        long objPerSec = MESSAGE_COUNT / timeInMS * 1000;
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
        int BYTE_BUFFER_CAPACITY = 16 << 10;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BYTE_BUFFER_CAPACITY);

        int hashSum = 0;
        int counter = 0;

        BlockingQueue<Integer> result = new ArrayBlockingQueue<>(1);

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
                }
            });
        }

        private void deserializeFromBuffer() {
            byteBuffer.flip();
            while (byteBuffer.hasRemaining()) {
                int objHash = kryoSerializer.deserializeFromByteBuffer(byteBuffer).hashCode();
                hashSum += objHash;
                counter++;
            }

            if (counter < MESSAGE_COUNT) {
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
            threadPool.shutdown();
        }
    }

    public static class NIO2Client {

        int counter = 0;
        int hashSum = 0;

        AsynchronousChannelGroup threadPool;
        AsynchronousSocketChannel server;

        KryoSerializer kryoSerializer = new KryoSerializer();
        int BYTE_BUFFER_CAPACITY = 16 << 10;
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
            serializeMessages();
        }

        private void serializeMessages() {
            for (; counter < MESSAGE_COUNT; counter++) {
                if (byteBuffer.position() > 0.9 * BYTE_BUFFER_CAPACITY) {
                    writeToSocket();
                    break;
                }
                Edge obj = new Edge(rnd.nextInt(10_000), rnd.nextInt(10_000));
                hashSum += obj.hashCode();
                kryoSerializer.serializeToByteBuffer(obj, byteBuffer);
            }
        }

        private void writeToSocket() {
            byteBuffer.flip();
            server.write(byteBuffer, null, new CompletionHandler<Integer, Object>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    byteBuffer.compact();
                    if (counter < MESSAGE_COUNT) {
                        serializeMessages();
                    } else if (counter == MESSAGE_COUNT && byteBuffer.position() > 0) {
                        writeToSocket();
                    }
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                }
            });
        }

        public void terminate() {
            this.threadPool.shutdown();
        }
    }


}
