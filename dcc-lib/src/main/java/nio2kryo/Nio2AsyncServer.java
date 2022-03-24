package nio2kryo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import until.serialization.async.AsyncSerializer;

public class Nio2AsyncServer {

	public static final int DEFAULT_PORT = 8439;

	public static Logger LOGGER = LoggerFactory
			.getLogger(Nio2AsyncServer.class);

	public static void main(String[] args)
			throws IOException, InterruptedException {

		int port = NioKryoServer.DEFAULT_PORT;

		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}

		LOGGER.info("port = {}.", port);

		AsyncSerializer ser = new AsyncSerializer();
		ser.register(Edge.class);

		AsynchronousChannelGroup group = AsynchronousChannelGroup
				.withFixedThreadPool(1, Executors.defaultThreadFactory());
		AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel
				.open(group);
		serverChannel.bind(new InetSocketAddress(port));
		serverChannel.accept(null,
				new CompletionHandler<AsynchronousSocketChannel, Object>() {

					@Override
					public void completed(
							AsynchronousSocketChannel clientChannel,
							Object attachment) {

						if (serverChannel.isOpen())
							serverChannel.accept(null, this);

						if (clientChannel == null || !clientChannel.isOpen()) {
							return;
						}

						LOGGER.info("Client connected!");

						new Nio2AsyncPublisher<>(clientChannel, ser).subscribe(
								new Nio2AsyncSubscriber<>(clientChannel, ser));

					}

					@Override
					public void failed(Throwable exc, Object attachment) {
						// TODO: handle
						exc.printStackTrace();
					}
				});
		while (!group.isTerminated()) {
			group.awaitTermination(1, TimeUnit.SECONDS);
		}

	}

}
