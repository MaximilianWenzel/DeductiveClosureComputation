package networking.reactor.netty.echo;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Predicate;

import networking.reactor.netty.echo.ReactorNettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;

public class ReactorNettyClient {

	public static Logger LOGGER = LoggerFactory
			.getLogger(ReactorNettyClient.class);

	// public static final int COUNT = 100_000_000;
	public static final int COUNT = 1_000_000;

	// much faster with batches, don't know why
	// uncomment batch processing in the stream
	public static final int BATCH_SIZE = 128;

	public static void main(String[] args) {

		Random rnd = new Random();

		// the stream of random objects that will be sent over network and back
		Flux<Object> stream = Flux.range(1, COUNT)
				.map(ignore -> new networking.react.netty.echo.Edge(rnd.nextInt(1000), rnd.nextInt(1000)));

		LoopResources loop = LoopResources.create("event-loop", 1, true);

		Connection connection = TcpClient.create().runOn(loop)
				.port(ReactorNettyServer.PORT)
				// .doOnConnected(conn -> conn.addHandler(
				// new ReadTimeoutHandler(10, TimeUnit.SECONDS)))
				.doOnChannelInit((observer, channel, remoteAddress) -> {
					channel.pipeline().addFirst(new networking.react.netty.echo.NettyKryoEncoder(),
							new networking.react.netty.echo.NettyKryoDecoder());
				})

				.handle((inbound, outbound) -> {
					int[] counts = new int[2]; // send and received objects
					int[] hashDiff = new int[1]; // sent - received hashes
					long[] timeDiff = new long[1]; // time duration
					timeDiff[0] -= System.currentTimeMillis();
					return outbound
							.sendObject(Flux.concat(stream.doOnNext(obj -> {
								int h = obj.hashCode();
								counts[0]++;
								hashDiff[0] += h;
							})
					// UNCOMMENT FOR BATCHES
//					 .buffer(BATCH_SIZE).map(l -> l.toArray())// batches
					, Mono.just("done"))).then()
							.and(inbound.receiveObject()
									.takeWhile(Predicate.not("done"::equals))
									// UNCOMMENT FOR BATCHES
//									 .flatMapIterable(
//									 arr -> Arrays.asList((Object[]) arr))
									.doOnNext(obj -> {
										int h = obj.hashCode();
										counts[1]++;
										hashDiff[0] -= h;
									}).doOnComplete(() -> {
										timeDiff[0] += System
												.currentTimeMillis();
										if (counts[0] != counts[1]
												|| counts[0] != COUNT) {
											LOGGER.error(
													"Sent: {}, received {} of total {}!",
													counts[0], counts[1],
													COUNT);
										}
										if (hashDiff[0] != 0) {
											LOGGER.error(
													"Send/receive hash mismatch!");
										}
										LOGGER.info(
												"Sent {} objects in {} ms. ({} obj/sec)",
												COUNT, timeDiff[0],
												COUNT / timeDiff[0] * 1000);
									}).then())
							.doOnError(e -> LOGGER.error("Error", e));
				}).connectNow();

		connection.onDispose().block();

	}

}
