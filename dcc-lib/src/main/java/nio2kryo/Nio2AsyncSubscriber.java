package nio2kryo;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;

import until.serialization.async.AsyncSerializer;

public class Nio2AsyncSubscriber<T>
		implements Flow.Subscriber<T>, CompletionHandler<Integer, Void> {

	/**
	 * The channel to which data is written.
	 */
	private final AsynchronousSocketChannel channel_;

	private final AsyncSerializer serializer_;

	private final ByteBuffer buf_;

	public Nio2AsyncSubscriber(AsynchronousSocketChannel channel,
			AsyncSerializer serializer, int bufferSize) {
		this.channel_ = channel;
		this.serializer_ = serializer;
		this.buf_ = ByteBuffer.allocateDirect(bufferSize);
		Nio2EndOfStream.registerWith(serializer);
	}

	public Nio2AsyncSubscriber(AsynchronousSocketChannel channel,
			AsyncSerializer serializer) {
		this(channel, serializer, Nio2AsyncPublisher.DEFAULT_BUFFER_SIZE);
	}

	/**
	 * The subscription of this subscriber; should be only one
	 */
	private Subscription subscription_ = null;

	@Override
	public void onSubscribe(Subscription subscription) {
		if (this.subscription_ != null) {
			throw new IllegalArgumentException(
					"Already subscribed to " + this.subscription_);
		}
		this.subscription_ = subscription;
		subscription_.request(1); // may call onNext immediately
	}

	@Override
	public void onNext(T item) {
		write(item);
	}

	private Runnable pendingWrites_ = null;
	// Avoid (unbound) recursion onNext -> request -> onNext -> request ..
	private boolean callingWrite_ = false;
	private Object next;
	private boolean asyncWrite_ = false;

	void write(Object o) {
		next = o; // save to process in the loop instead of recursion
		if (callingWrite_) {
			callingWrite_ = false;
			return; // continues in the loop
		}
		try {
			while (!callingWrite_) {
				callingWrite_ = true;
				serializer_.writeClassAndObject(buf_, next);
				handleWrites();
				if (asyncWrite_) {
					return;
				}
			}
		} finally {
			callingWrite_ = false;
		}
	}

	void handleWrites() {
		pendingWrites_ = serializer_.pendingOperations();
		if (pendingWrites_ == null && !isComplete_) {
			subscription_.request(1); // may call onNext immediately
		} else if (buf_.position() > 0) {
			buf_.flip();
			asyncWrite_ = true;
			channel_.write(buf_, null, this); // may complete here
		}
	}

	private boolean completingWrite_ = false;

	@Override
	public void completed(Integer result, Void attachment) {
		asyncWrite_ = false;
		if (completingWrite_) {
			completingWrite_ = false; // will continue in the loop
			return;
		}
		try {
			while (!completingWrite_) {
				completingWrite_ = true;
				buf_.compact();
				if (pendingWrites_ != null) {
					pendingWrites_.run();
				}
				handleWrites(); // may call completed
			}
		} finally {
			completingWrite_ = false;
		}
	}

	@Override
	public void failed(Throwable exc, Void attachment) {
		// TODO: handle
		exc.printStackTrace();

	}

	@Override
	public void onError(Throwable throwable) {
		// TODO: handle
		throwable.printStackTrace();
	}

	/**
	 * The flag indicating that the subscription is complete, so no further data
	 * comes from upstream.
	 */
	private boolean isComplete_ = false;

	@Override
	public void onComplete() {
		isComplete_ = true;
		write(Nio2EndOfStream.get());
	}

}
