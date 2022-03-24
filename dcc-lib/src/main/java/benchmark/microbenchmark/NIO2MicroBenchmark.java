package benchmark.microbenchmark;

import com.google.common.base.Stopwatch;
import nio2kryo.Edge;
import util.ConsoleUtils;
import util.NetworkingUtils;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NIO2MicroBenchmark {

    public static final int MESSAGE_COUNT = 20_000_000;
    public static final Random rnd = new Random();
    public static final int BUFFER_SIZE = 512 << 10;

    public static void main(String[] args) {
        /*
        Scanner s = new Scanner(System.in);
        s.nextLine();

         */
        for (int i = 0; i < 3; i++) {
            executeExperiment();
            System.out.println(ConsoleUtils.getSeparator());
        }
    }

    public static void executeExperiment() {
        NIO2Server server = new NIO2Server();
        NIO2Client client = new NIO2Client(server.port);

        Stopwatch sw = Stopwatch.createStarted();
        try {
            client.run();
            client.getChannelGroup().awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sw.stop();

        int hashCodeServer = server.getResult();
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
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

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

    public static class NIO2Client implements Runnable {

        AtomicInteger counter = new AtomicInteger(0);
        int hashSum = 0;

        AsynchronousChannelGroup channelGroup;
        AsynchronousSocketChannel server;

        KryoSerializer kryoSerializer = new KryoSerializer();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

        Serializable lastObj;
        Serializable nextObj;

        public NIO2Client(int serverPort) {
            try {
                this.channelGroup = AsynchronousChannelGroup.withFixedThreadPool(1, Thread::new);

                InetSocketAddress hostAddress = new InetSocketAddress("localhost", serverPort);
                server = AsynchronousSocketChannel.open(channelGroup);
                server.connect(hostAddress).get();
            } catch (IOException | ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            serializeMessages();
        }

        private void serializeMessages() {
            for (; counter.get() < MESSAGE_COUNT; counter.incrementAndGet()) {
                if (lastObj != null) {
                    nextObj = lastObj;
                    lastObj = null;
                } else {
                    nextObj = new Edge(rnd.nextInt(10_000), rnd.nextInt(10_000));
                }

                hashSum += nextObj.hashCode();
                if (!serializeObjToBuffer(nextObj)) {
                    // buffer is full
                    lastObj = nextObj;
                    return;
                }
            }
            writeToSocket();
        }

        private boolean serializeObjToBuffer(Serializable obj) {
            if (byteBuffer.position() < 0.9 * BUFFER_SIZE) {
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
                return true;

            } else {
                writeToSocket();
                return false;
            }
        }

        private void writeToSocket() {
            byteBuffer.flip();
            server.write(byteBuffer, null, new CompletionHandler<Integer, Object>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    byteBuffer.compact();

                    if (counter.get() < MESSAGE_COUNT) {
                        serializeMessages();
                    } else if (counter.get() == MESSAGE_COUNT && byteBuffer.position() > 0) {
                        writeToSocket();
                    } else {
                        terminate();
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
                this.channelGroup.shutdownNow();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public AsynchronousChannelGroup getChannelGroup() {
            return channelGroup;
        }
    }


}
