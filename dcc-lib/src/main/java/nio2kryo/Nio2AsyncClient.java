package nio2kryo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import until.serialization.async.AsyncSerializer;

public class Nio2AsyncClient {

	public static Logger LOGGER = LoggerFactory
			.getLogger(Nio2AsyncClient.class);

	public static String DEFAULT_HOST = "127.0.0.1";

	public static final int COUNT = 500_000_000;
//	 public static final int COUNT = 1_000_000;
//	 public static final int COUNT = 100_000;
//	 public static final int COUNT = 100;

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

		AsyncSerializer ser = new AsyncSerializer();
		ser.register(Edge.class);

		AsynchronousChannelGroup group = AsynchronousChannelGroup
				.withFixedThreadPool(1, Executors.defaultThreadFactory());
		AsynchronousSocketChannel clientChannel = AsynchronousSocketChannel
				.open(group);

		clientChannel.connect(new InetSocketAddress(host, port), null,
				new CompletionHandler<Void, Object>() {

					@Override
					public void completed(Void result, Object attachment) {

						LOGGER.info("Connected to server!");

						EdgeProcessor p = new EdgeProcessor(clientChannel,
								COUNT);

						p.subscribe(
								new Nio2AsyncSubscriber<>(clientChannel, ser));

						new Nio2AsyncPublisher<Edge>(clientChannel, ser)
								.subscribe(p);

					}

					@Override
					public void failed(Throwable exc, Object attachment) {
						throw new RuntimeException(exc);
					}

				});

		while (!group.isTerminated()) {
			if (!clientChannel.isOpen()) {
				group.shutdown();
			}
			group.awaitTermination(1, TimeUnit.SECONDS);
		}

	}

	static class EdgeProcessor
			implements Flow.Processor<Edge, Edge>, Flow.Subscription {

		private final AsynchronousSocketChannel channel_;
		private final int limit_;

		private int hashDiff_;
		private long duration_;

		public EdgeProcessor(AsynchronousSocketChannel channel, int limit) {
			this.channel_ = channel;
			this.limit_ = limit;
		}

		/* Publishing Edges */

		private final Random rnd_ = new Random();
		private Subscriber<? super Edge> subscriber_;

		@Override
		public void subscribe(Subscriber<? super Edge> subscriber) {
			if (subscriber_ == null) {
				this.subscriber_ = subscriber;
			} else {
				throw new IllegalStateException(
						"Only one subscriber supported");
			}
			subscriber.onSubscribe(this);
			duration_ = -System.currentTimeMillis();
		}

		/* Subscription */

		/**
		 * The number of items currently requested but not yet published to the
		 * subscribers.
		 */
		private long requested_ = 0;
		private long published_ = 0;

		@Override
		public void request(long n) {
			requested_ += n;
			if (requested_ > limit_) {
				requested_ = limit_;
			}
			dispatchOnNext();
		}

		/* avoiding recursive calls */
		private boolean dispatchingOnNext_ = false;

		private void dispatchOnNext() {
			if (dispatchingOnNext_) {
				return; // already inside the loop
			}
			try {
				dispatchingOnNext_ = true;
				while (published_ < requested_) {
					Edge next = generate();
					// LOGGER.info("Sending: {}", next);
					subscriber_.onNext(next); // may call request and dispatch
					published_++;
					hashDiff_ += next.hashCode();
					if (published_ == limit_) {
						LOGGER.info("All sent!");
						subscriber_.onComplete();
						return;
					}
				}
			} finally {
				dispatchingOnNext_ = false;
			}
		}

		Edge generate() {
			return new Edge(rnd_.nextInt(1000), rnd_.nextInt(1000));
		}

		@Override
		public void cancel() {
			requested_ = 0;
		}

		/* Subscriber */

		private int received_;

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Edge item) {
			// LOGGER.info("Received: {}", item);
			received_++;
			hashDiff_ -= item.hashCode();
		}

		@Override
		public void onError(Throwable throwable) {
			// TODO: Handle
			throwable.printStackTrace();
		}

		@Override
		public void onComplete() {
			LOGGER.info("All received!");
			duration_ += System.currentTimeMillis();
			if (published_ != received_ || published_ != limit_) {
				LOGGER.error("Sent: {}, received {} of total {}!", published_,
						received_, limit_);
			}
			if (hashDiff_ != 0) {
				LOGGER.error("Send/receive hash mismatch!");
			}
			LOGGER.info("Sent {} objects in {} ms. ({} obj/sec)", published_,
					duration_, limit_ / duration_ * 1000);
			try {
				channel_.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
