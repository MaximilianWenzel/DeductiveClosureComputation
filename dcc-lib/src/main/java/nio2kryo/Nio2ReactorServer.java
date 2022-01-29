package nio2kryo;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;

public class Nio2ReactorServer {

	public static final int DEFAULT_PORT = 8439;

	public static Logger LOGGER = LoggerFactory
			.getLogger(Nio2ReactorServer.class);

	public static void main(String[] args)
			throws IOException, InterruptedException {

		int port = NioKryoServer.DEFAULT_PORT;

		if (args.length > 1) {
			port = Integer.parseInt(args[1]);
		}

		LOGGER.info("port = {}.", port);

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

						Publisher<Serializable> socketPublisher = FlowAdapters
								.toPublisher(new Nio2ChannelPublisher<>(
										clientChannel));

						Subscriber<Serializable> socketSubscriber = FlowAdapters
								.toSubscriber(new Nio2ChannelSubscriber<>(
										clientChannel));

						// echo!
						Flux.from(socketPublisher).takeUntil("done"::equals)
								.doOnComplete(
										() -> LOGGER.info("All forwarded!"))
								.doOnError(e -> e.printStackTrace())
								.subscribe(socketSubscriber);

						// .subscribe();

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
