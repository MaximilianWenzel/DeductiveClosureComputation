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

	/**
	 * The flag indicated that the content of the buffer is being flushed to the
	 * channel. No data should be written to the buffer during this time.
	 */
	private boolean flushing_ = false;

	/**
	 * The flag indicating that there is an (asynchronous) pending write
	 * operation of the buffer to the output channel: {@link #pendingWrite_} =
	 * {@code true} implies {@link #flushing_} = {@code true}.
	 */
	private boolean pendingWrite_ = false;

	/**
	 * The flag indicating that the subscription is complete, so no further data
	 * comes from the upstream.
	 */
	private boolean isComplete_ = false;

	/**
	 * Creates {@link Subscriber} that writes received the messages to the given
	 * socket channel.
	 * 
	 * @param channel
	 *            the channel to which to write the messages
	 */
	public Nio2ChannelSubscriber(AsynchronousSocketChannel channel) {
		this(channel, MAX_BUFFER_SIZE);
	}

	/**
	 * Creates {@link Subscriber} that writes received the messages to the given
	 * socket channel.
	 * 
	 * @param channel
	 *            the channel to which to write the messages
	 * @param bufferSize
	 *            the number of bytes used for storing serialized messages
	 *            before they are sent to the channel; one fourth of this value
	 *            must be sufficient to store the serialized message sent to
	 *            this subscriber.
	 */
	public Nio2ChannelSubscriber(AsynchronousSocketChannel channel, int bufferSize) {
		if (bufferSize > MAX_BUFFER_SIZE) {
			throw new IllegalArgumentException(
					"The buffer size cannot exceed " + MAX_BUFFER_SIZE);
		}
		this.channel_ = channel;
		kryoOutput_ = new ByteBufferOutput(
				ByteBuffer.allocateDirect(bufferSize));
		this.minBufferFreeBytes_ = this.minBufferSentBytes_ = bufferSize >> 2;
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
		initWriting();
	}

	private void initWriting() {
		if (isComplete_) {
			return;
		}
		kryoOutput_.setPosition(startWritePos_ + SIZE_BYTES);
		subscription_.request(1); // may call onNext immediately
	}

	// Avoid (unbound) recursion onNext -> request -> onNext -> request ..
	private boolean callingOnNext_ = false;
	private T next;

	@Override
	public void onNext(T item) {
		if (flushing_) {
			throw new IllegalStateException(
					"Cannot process items while flushing!");
		}
		next = item; // save to process in the loop instead of recursion
		if (callingOnNext_) {
			callingOnNext_ = false;
			return; // continues in the loop
		}
		try {
			while (!callingOnNext_) {
				callingOnNext_ = true;
				KryoConfig.get().writeClassAndObject(kryoOutput_, next);
				if (kryoOutput_.getByteBuffer()
						.remaining() < minBufferFreeBytes_) {
					flush();
					if (pendingWrite_) {
						return; // will finish flushing later
					}
				} else {
					subscription_.request(1); // may immediately call onNext
				}
			}
		} finally {
			callingOnNext_ = false;
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
		if (flushing_) {
			throw new IllegalStateException("Already flushing!");
		}
		int end = kryoOutput_.position();
		int writtenBytes = end - startWritePos_ - SIZE_BYTES;
		if (writtenBytes == 0) {
			throw new IllegalStateException(
					"Flushing buffer but nothing was written!");
		}
		kryoOutput_.setPosition(startWritePos_);
		kryoOutput_.writeShort(writtenBytes);
		kryoOutput_.setPosition(end);
		kryoOutput_.getByteBuffer().flip();
		writeToChannel();
	}

	private void writeToChannel() {
		ByteBuffer buf = kryoOutput_.getByteBuffer();
		while (!flushing_ && buf.hasRemaining()
				&& (isComplete_ || buf.position() < minBufferSentBytes_)) {
			flushing_ = true;
			channel_.write(buf, null, this);
		}
		if (flushing_) {
			pendingWrite_ = true;
			return; // will complete later
		}
		buf.compact();
		kryoOutput_.setBuffer(buf);
		startWritePos_ = kryoOutput_.position();
		initWriting(); // may call flush()
	}

	@Override
	public void completed(Integer written, Void attachment) {
		flushing_ = false;
		if (pendingWrite_) { // continue writing to channel
			pendingWrite_ = false;
			writeToChannel();
		}
	}

	@Override
	public void failed(Throwable exc, Void attachment) {
		// TODO: handle
		exc.printStackTrace();
	}

}