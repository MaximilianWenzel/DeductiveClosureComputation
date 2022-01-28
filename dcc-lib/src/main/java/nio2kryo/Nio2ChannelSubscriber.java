package nio2kryo;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import com.esotericsoftware.kryo.io.ByteBufferOutput;

/**
 * A {@link Subscriber} that serializes the received messages using KRYO writes
 * them to an {@link AsynchronousSocketChannel}.
 * 
 * @author Yevgeny Kazakov
 *
 * @param <T>
 *            the subscribed item type
 * 
 * @see Nio2ChannelPublisher
 */
public class Nio2ChannelSubscriber<T extends Serializable>
		implements Flow.Subscriber<T>, CompletionHandler<Integer, Void> {

	// TODO: move constants to a central location

	/**
	 * Number of bytes to hold the size information.
	 */
	public static final int SIZE_BYTES = 2; // for writing as unsigned short.
	/**
	 * The maximal unsigned value encoded using {@value #SIZE_BYTES} bytes.
	 */
	public static final int MAX_BUFFER_SIZE = 1 << (8 * SIZE_BYTES);
	/**
	 * The channel to which data is written.
	 */
	private final AsynchronousSocketChannel channel_;
	/**
	 * The buffer into which the data is serialized before flushing to channel.
	 */
	private final ByteBufferOutput kryoOutput_;

	/**
	 * The minimal number of free bytes available in the buffer to perform
	 * serialization. This should be at least the number of bytes to serialize
	 * any message received by this subscriber.
	 */
	private final int minBufferFreeBytes_;
	/**
	 * The minimal number of bytes that should be sent to the socket before
	 * writing to the buffer again.
	 */
	private final int minBufferSentBytes_;
	/**
	 * Marks the buffer position starting from which the current batch of
	 * messages has been serialized.
	 */
	private int startWritePos_ = 0;

	private boolean isComplete_ = false;

	Nio2ChannelSubscriber(AsynchronousSocketChannel channel) {
		this(channel, MAX_BUFFER_SIZE);
	}

	Nio2ChannelSubscriber(AsynchronousSocketChannel channel, int bufferSize) {
		if (bufferSize > MAX_BUFFER_SIZE) {
			throw new IllegalArgumentException(
					"The buffer size cannot exceed " + MAX_BUFFER_SIZE);
		}
		this.channel_ = channel;
		kryoOutput_ = new ByteBufferOutput(
				ByteBuffer.allocateDirect(bufferSize));
		this.minBufferFreeBytes_ = this.minBufferSentBytes_ = bufferSize >> 2;
	}

	private Subscription subscription_ = null;

	@Override
	public void onSubscribe(Subscription subscription) {
		if (this.subscription_ != null) {
			throw new IllegalArgumentException(
					"Already subscribed to " + this.subscription_);
		}
		this.subscription_ = subscription;
		readItems();
	}

	private void readItems() {
		if (isComplete_) {
			return;
		}
		kryoOutput_.setPosition(startWritePos_ + SIZE_BYTES);
		// TODO: request more items and buffer them
		subscription_.request(1);
	}

	@Override
	public void onNext(T item) {
		KryoConfig.get().writeClassAndObject(kryoOutput_, item);
		if (kryoOutput_.getByteBuffer().remaining() > minBufferFreeBytes_) {
			subscription_.request(1);
		} else {
			flush();
		}
	}

	@Override
	public void onError(Throwable throwable) {
		// TODO: handle
		throwable.printStackTrace();
	}

	@Override
	public void onComplete() {
		isComplete_ = true;
		flush();
	}

	private void flush() {
		int end = kryoOutput_.position();
		kryoOutput_.setPosition(startWritePos_);
		int writtenBytes = end - startWritePos_ - SIZE_BYTES;
		if (writtenBytes == 0) {
			throw new RuntimeException(
					"Flushing buffer but nothing was written!");
		}
		kryoOutput_.writeShort(writtenBytes);
		kryoOutput_.setPosition(end);
		ByteBuffer buf = kryoOutput_.getByteBuffer();
		buf.flip();
		channel_.write(buf, null, this);
	}

	/**
	 * A flag to verify if the callback was made immediately in the same thread.
	 * This is to avoid potential stack overflow.
	 */
	private boolean immediateCallback_ = false;

	@Override
	public void completed(Integer written, Void attachment) {
		try {
			if (immediateCallback_) {
				return; // will be continued in the loop
			}
			while (!immediateCallback_) {
				ByteBuffer buf = kryoOutput_.getByteBuffer();
				immediateCallback_ = true; // to continue in the loop
				while (buf.hasRemaining() && (isComplete_
						|| buf.position() < minBufferSentBytes_)) {
					channel_.write(buf, null, this);
					if (immediateCallback_) {
						// callback was not called immediately, continue later
						return;
					}
				}
				buf.compact();
				kryoOutput_.setBuffer(buf);
				startWritePos_ = kryoOutput_.position();
				readItems(); // may call flush() and this (blocked) callback
				// so instead continue in the loop
			}
		} finally {
			immediateCallback_ = false; // left the loop, callback as usual
		}
	}

	@Override
	public void failed(Throwable exc, Void attachment) {
		// TODO: handle
		exc.printStackTrace();
	}

}