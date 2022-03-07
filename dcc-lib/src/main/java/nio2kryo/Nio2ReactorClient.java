package nio2kryo;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Nio2ReactorClient {

	public static Logger LOGGER = LoggerFactory
			.getLogger(Nio2ReactorClient.class);

	public static String DEFAULT_HOST = "127.0.0.1";

	public static final int COUNT = 100_000_000;
	// public static final int COUNT = 1_000_000;

	public static void main(String[] args)
			throws IOException, InterruptedException {

		String host = DEFAULT_HOST;

		if (args.length > 0) {
			host = args[0];
		}

		int port = Nio2ReactorServer.DEFAULT_PORT;

		if (args.length > 1) {
			port = Integer.parseInt(args[1]);
		}

		LOGGER.info("host = {}.", host);
		LOGGER.info("port = {}.", port);

		Random rnd = new Random(123); // reproducible runs

		Flux<Serializable> stream = Flux.range(1, COUNT)
				.map(ignore -> new Edge(rnd.nextInt(1000),
						rnd.nextInt(1000)));

		AsynchronousChannelGroup group = AsynchronousChannelGroup
				.withFixedThreadPool(1, Executors.defaultThreadFactory());
		AsynchronousSocketChannel clientChannel = AsynchronousSocketChannel
				.open(group);

		clientChannel.connect(new InetSocketAddress(host, port), null,
				new CompletionHandler<Void, Object>() {

					@Override
					public void completed(Void result, Object attachment) {

						LOGGER.info("Connected to server!");

						int[] counts = new int[2]; // send and received objects
						int[] hashDiff = new int[1]; // sent - received hashes
						long[] timeDiff = new long[1]; // time duration
						timeDiff[0] -= System.currentTimeMillis();

						Subscriber<Serializable> socketSubscriber = FlowAdapters
								.toSubscriber(new Nio2ChannelSubscriber<>(
										clientChannel));

						stream.doOnNext(obj -> {
							int h = obj.hashCode();
							counts[0]++;
							hashDiff[0] += h;
						}).concatWith(Mono.just("done"))
								.doOnComplete(() -> LOGGER.info("All sent!"))
								.subscribe(socketSubscriber);

						Publisher<Serializable> publisher = FlowAdapters
								.toPublisher(new Nio2ChannelPublisher<>(
										clientChannel));

						Flux.from(publisher)
								.takeWhile(Predicate.not("done"::equals))
								.doOnNext(obj -> {
									int h = obj.hashCode();
									counts[1]++;
									hashDiff[0] -= h;
								}).doOnComplete(() -> {
									timeDiff[0] += System.currentTimeMillis();
									LOGGER.info("All received!");
									if (counts[0] != counts[1]
											|| counts[0] != COUNT) {
										LOGGER.error(
												"Sent: {}, received {} of total {}!",
												counts[0], counts[1], COUNT);
									}
									if (hashDiff[0] != 0) {
										LOGGER.error(
												"Send/receive hash mismatch!");
									}
									LOGGER.info(
											"Sent {} objects in {} ms. ({} obj/sec)",
											COUNT, timeDiff[0],
											COUNT / timeDiff[0] * 1000);
									try {
										clientChannel.close();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									group.shutdown();

								}).subscribe();
					}

					@Override
					public void failed(Throwable exc, Object attachment) {
						throw new RuntimeException(exc);
					}

				});

		while (!group.isTerminated()) {
			group.awaitTermination(1, TimeUnit.SECONDS);
		}

	}

}
