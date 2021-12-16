package nio2kryo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Nio2KryoServer {

	public static final int DEFAULT_PORT = 8439;

	public final int port;

	Nio2KryoServer(int port) {
		this.port = port;
	}

	Nio2KryoServer() {
		this(DEFAULT_PORT);
	}

	public void start() throws IOException, InterruptedException {
		AsynchronousChannelGroup group = AsynchronousChannelGroup
				.withFixedThreadPool(1, Executors.defaultThreadFactory());
		AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel
				.open(group);
		serverChannel.bind(new InetSocketAddress(port));
		serverChannel.accept(null,
				new CompletionHandler<AsynchronousSocketChannel, Object>() {

					@Override
					public void completed(AsynchronousSocketChannel result,
							Object attachment) {
						if (serverChannel.isOpen())
							serverChannel.accept(null, this);
						AsynchronousSocketChannel clientChannel = result;
						if ((clientChannel != null)
								&& (clientChannel.isOpen())) {
							Nio2ObjectChannel client = new Nio2ObjectChannel(
									clientChannel);
							ReadWriteContext context = new ReadWriteContext(
									client);
							context.readWriteIfNeeded();
						}
					}

					@Override
					public void failed(Throwable exc, Object attachment) {
						// process error
					}
				});
		while (!group.isTerminated()) {
			group.awaitTermination(1, TimeUnit.SECONDS);
		}
	}

	static class ReadWriteContext implements Reader, Writer {

		final Nio2ObjectChannel client;

		Object[] buffer = new Object[4096 * 4];

		int readPos = 0, size = 0;

		ReadWriteContext(Nio2ObjectChannel client) {
			this.client = client;
		}

		@Override
		public boolean canWrite() {
			return size < buffer.length;
		}

		@Override
		public void write(Object o) {
			// System.out.println("Received: " + o);
			int writePos = readPos + size;
			if (writePos >= buffer.length) {
				writePos -= buffer.length;
			}
			buffer[writePos] = o;
			size++;
		}

		@Override
		public boolean canRead() {
			return size > 0;
		}

		@Override
		public Object read() {
			Object result = buffer[readPos];
			// System.out.println("Sent: " + result);
			buffer[readPos] = null;
			size--;
			readPos++;
			if (readPos == buffer.length) {
				readPos = 0;
			}
			return result;
		}

		void close() {
			try {
				client.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		boolean writing = false;
		boolean reading = false;
		boolean allSent = false;

		void writeIfNeeded() {
			if (writing) {
				return;
			}
			writing = true;
			client.write(this, null, writeHandler);
		}

		void readIfNeeded() {
			if (reading) {
				return;
			}
			reading = true;
			client.read(this, null, readHandler);
		}

		void readWriteIfNeeded() {
			if (canRead() || !allSent) {
				writeIfNeeded();
			}
			if (canWrite()) {
				readIfNeeded();
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

		int port = Nio2KryoServer.DEFAULT_PORT;

		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}

		System.out.println("port = " + port);

		new Nio2KryoServer(port).start();
	}

}