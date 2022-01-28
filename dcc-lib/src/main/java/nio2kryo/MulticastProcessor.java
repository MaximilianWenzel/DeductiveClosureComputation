package nio2kryo;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

/**
 * A {@link Processor} that requests only the minimal number of items requested
 * by downstream subscribers and delegates the upstream items to all of
 * subscribers.
 * 
 * TODO: maybe there is a ready class that does this.
 * 
 * @author Yevgeny Kazakov
 *
 * @param <T>
 *            the subscribed and published item types
 */
public class MulticastProcessor<T> implements Flow.Processor<T, T> {

	private static final int INITIAL_SUBSCRIPTION_CAPACITY_ = 4;

	/**
	 * The number of registered subscribers.
	 */
	private int subscriberCount_;
	/**
	 * The resizable array holding the subscribers.
	 */
	private Subscriber<?>[] subscribers_ = new Subscriber[INITIAL_SUBSCRIPTION_CAPACITY_];
	/**
	 * Downstream requests that were not yet forwarded to the upstream.
	 */
	private long[] pendingRequests_ = new long[INITIAL_SUBSCRIPTION_CAPACITY_];
	/**
	 * The subscriber that made the minimum number of requests.
	 */
	private int minSubscriberId_ = 0;
	/**
	 * The upstream subscription to which the requests are forwarded.
	 */
	private Subscription upstreamSubscription_;

	@Override
	public void onSubscribe(Subscription subscription) {
		if (upstreamSubscription_ != null) {
			throw new IllegalArgumentException(
					"Only one upstream subscription allowed!");
		}
		this.upstreamSubscription_ = subscription;
	}

	@Override
	public void subscribe(Subscriber<? super T> subscriber) {
		if (subscribers_.length == subscriberCount_) {
			// the arrays a full, increase the capacity
			int newCapacity = subscribers_.length << 2;
			Subscriber<?>[] newSubscribers = new Subscriber[newCapacity];
			long[] newPendingRequests = new long[newCapacity];
			for (int i = 0; i < subscribers_.length; i++) {
				newSubscribers[i] = subscribers_[i];
				newPendingRequests[i] = pendingRequests_[i];
			}
			subscribers_ = newSubscribers;
			pendingRequests_ = newPendingRequests;
		}
		subscribers_[subscriberCount_] = subscriber;
		Subscription subscription = new DownstreamSubscription(
				subscriberCount_);
		subscriberCount_++;
		subscriber.onSubscribe(subscription);
	}

	/**
	 * The subscription for registered subscribers.
	 * 
	 * @author Yevgeny Kazakov
	 */
	private class DownstreamSubscription implements Flow.Subscription {

		private final int id_;

		DownstreamSubscription(int id) {
			this.id_ = id;
		}

		@Override
		public void request(long n) {
			if (n <= 0) {
				throw new IllegalArgumentException(
						"Requested number must be positive:  " + n);
			}
			pendingRequests_[id_] += n;
			if (id_ != minSubscriberId_) {
				return; // cannot forward the requests yet
			}
			// calculated the minimal request of downstream subscribers
			long min = Long.MAX_VALUE;
			for (int i = 0; i < subscriberCount_; i++) {
				long next = pendingRequests_[i];
				if (next < min) {
					min = next;
				}
			}
			// reduce all downstream requests by this value
			for (int i = 0; i < subscriberCount_; i++) {
				if ((pendingRequests_[i] -= min) == 0) {
					minSubscriberId_ = i;
				}
			}
			// make the upstream request
			upstreamSubscription_.request(min);
		}

		@Override
		public void cancel() {
			upstreamSubscription_.cancel();
		}

	}

	@Override
	public void onNext(T item) {
		for (int i = 0; i < subscriberCount_; i++) {
			@SuppressWarnings("unchecked")
			Subscriber<? super T> subscriber = (Subscriber<? super T>) subscribers_[i];
			subscriber.onNext(item);
		}
	}

	@Override
	public void onError(Throwable throwable) {
		for (int i = 0; i < subscriberCount_; i++) {
			subscribers_[i].onError(throwable);
		}
	}

	@Override
	public void onComplete() {
		for (int i = 0; i < subscriberCount_; i++) {
			subscribers_[i].onComplete();
		}
	}

}
