package nio2kryo;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;

import until.serialization.async.AsyncSerializer;

public class Nio2AsyncPublisher<T> implements Flow.Publisher<T>,
		Flow.Subscription, CompletionHandler<Integer, Void> {

	public static final int DEFAULT_BUFFER_SIZE = 1 << 20; // 1MB

	/**
	 * The channel from which data is read.
	 */
	private final AsynchronousSocketChannel channel_;

	private final AsyncSerializer serializer_;

	private final ByteBuffer buf_;

	public Nio2AsyncPublisher(AsynchronousSocketChannel channel,
			AsyncSerializer serializer, int bufferSize) {
		this.channel_ = channel;
		this.serializer_ = serializer;
		this.buf_ = ByteBuffer.allocateDirect(bufferSize);
		buf_.flip(); // switch to read mode
		Nio2EndOfStream.registerWith(serializer);
	}

	public Nio2AsyncPublisher(AsynchronousSocketChannel channel,
			AsyncSerializer serializer) {		
		this(channel, serializer, DEFAULT_BUFFER_SIZE);	
	}

	private Subscriber<? super T> subscriber_;

	@Override
	public void subscribe(Subscriber<? super T> subscriber) {
		if (subscriber_ != null) {
			throw new IllegalArgumentException(
					"This publisher can be subscribed only once!");
		}
		this.subscriber_ = subscriber;
		subscriber.onSubscribe(this);
	}

	/**
	 * The number of items currently requested but not yet published to the
	 * subscribers.
	 */
	private long requested_ = 0;

	/**
	 * The flag indicating whether this stream was completed
	 */
	private boolean isComplete_ = false;

	@Override
	public void request(long n) {
		requested_ += n;
		readItems();
	}

	@Override
	public void cancel() {
		requested_ = 0;
	}

	private Runnable pendingReads_ = null;
	private boolean readingItems_ = false;
	private boolean asyncRead_ = false;

	private void readItems() {
		if (readingItems_) {
			return;
		}
		try {
			readingItems_ = true;
			while (requested_ > 0 && !isComplete_) {
				if (pendingReads_ == null) {
					serializer_.readClassAndObject(buf_, this::onNext);
				} else {
					pendingReads_.run();
				}
				pendingReads_ = serializer_.pendingOperations();
				if (pendingReads_ != null) {
					buf_.compact();
					asyncRead_ = true;
					channel_.read(buf_, null, this); // may complete immediately
					if (asyncRead_) {
						break;
					}
				}
			}
		} finally {
			readingItems_ = false;
		}

	}

	@SuppressWarnings("unchecked")
	void onNext(Object item) {
		if (Nio2EndOfStream.is(item)) {
			isComplete_ = true;
			subscriber_.onComplete();
		} else {
			subscriber_.onNext((T) item);
			requested_--;
		}
	}

	@Override
	public void completed(Integer result, Void attachment) {
		asyncRead_ = false;
		buf_.flip();
		readItems();
	}

	@Override
	public void failed(Throwable exc, Void attachment) {
		// TODO: handle
		exc.printStackTrace();
	}

}
