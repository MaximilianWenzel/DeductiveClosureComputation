package nio2kryo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Nio2KryoClient {

	//public static int COUNT = 1000000000;
	public static int COUNT = 100000000;
//	public static int COUNT = 10000000;
//	public static int COUNT = 100000;
//	public static int COUNT = 10000;

	public static String DEFAULT_HOST = "127.0.0.1";

	private final String host;
	private final int port;

	int sentHash = 0, receivedHash = 0;

	Nio2KryoClient(String host, int port) throws IOException {
		this.host = host;
		this.port = port;
	}

	Nio2KryoClient() throws IOException {
		this(DEFAULT_HOST, NioKryoServer.DEFAULT_PORT);
	}

	public void run() throws IOException, InterruptedException {
		AsynchronousChannelGroup group = AsynchronousChannelGroup
				.withFixedThreadPool(1, Executors.defaultThreadFactory());
		AsynchronousSocketChannel clientChannel = AsynchronousSocketChannel
				.open(group);
		clientChannel.connect(new InetSocketAddress(host, port), null,
				new CompletionHandler<Void, Object>() {

					@Override
					public void completed(Void result, Object attachment) {
						Nio2ObjectChannel client = new Nio2ObjectChannel(
								clientChannel);
						ReadWriteContext context = new ReadWriteContext(client,
								group);
						client.write(context, null, context.writeHandler);
					}

					@Override
					public void failed(Throwable exc, Object attachment) {
						// TODO Auto-generated method stub

					}

				});
		while (!group.isTerminated()) {
			group.awaitTermination(1, TimeUnit.SECONDS);
		}
	}

	class ReadWriteContext implements Reader, Writer {

		private final Nio2ObjectChannel client;
		private final AsynchronousChannelGroup group;

		private int sent = 0, received = 0;

		private final Random rnd = new Random();

		ReadWriteContext(Nio2ObjectChannel clientChannel,
				AsynchronousChannelGroup group) {
			this.client = clientChannel;
			this.group = group;
		}

		@Override
		public boolean canWrite() {
			return received < COUNT;
		}

		@Override
		public boolean canRead() {
			return sent < COUNT;
		}

		@Override
		public void write(Object o) {
			receivedHash += o.hashCode();
			received++;
			// System.out.println("Received " + received + ": " + o);
		}

		@Override
		public Object read() {
			Object o = new Edge(rnd.nextInt(100000),
					rnd.nextInt(100000));
			// Object o = sent;
			sentHash += o.hashCode();
			sent++;
			// System.out.println("Sent " + sent + ": " + o);
			return o;
		}

		boolean writing = false;
		boolean reading = false;
		boolean allSent = false;

		void writeIfNeeded() {
			if (writing) {
				return;
			}
			writing = true;
			client.write(ReadWriteContext.this, null, writeHandler);
		}

		void readIfNeeded() {
			if (reading) {
				return;
			}
			reading = true;
			client.read(ReadWriteContext.this, null, readHandler);
		}

		void readWriteIfNeeded() {
			if (canRead() || !allSent) {
				writeIfNeeded();
			} else {
				// System.out.println("All sent!");
			}
			if (canWrite()) {
				readIfNeeded();
			} else {
				// System.out.println("All received!");
				try {
					client.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				group.shutdown();
			}
		}

		final CompletionHandler<Integer, Void> readHandler = new CompletionHandler<Integer, Void>() {

			@Override
			public void completed(Integer result, Void attachment) {
				reading = false;
				readWriteIfNeeded();
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub

			}

		};

		final CompletionHandler<Boolean, Void> writeHandler = new CompletionHandler<Boolean, Void>() {

			@Override
			public void completed(Boolean result, Void attachment) {
				writing = false;
				allSent = result;
				readWriteIfNeeded();
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub

			}

		};

	}

	public static void main(String[] args)
			throws IOException, InterruptedException {

		String host = DEFAULT_HOST;

		if (args.length > 0) {
			host = args[0];
		}

		int port = NioKryoServer.DEFAULT_PORT;

		if (args.length > 1) {
			port = Integer.parseInt(args[1]);
		}

		System.out.println("host = " + host);
		System.out.println("port = " + port);

		Nio2KryoClient client = new Nio2KryoClient(host, port);

		long time = 0;
		time -= System.currentTimeMillis();
		client.run();
		if (client.sentHash != client.receivedHash) {
			System.out.println("Hash mismatch: sentHash = " + client.sentHash
					+ " != " + client.receivedHash);
		}
		time += System.currentTimeMillis();
		System.out.println("Sent " + COUNT + " objects in " + time + " ms. ("
				+ (long) COUNT  * 1000 / time + " obj/sec)");

	}

}