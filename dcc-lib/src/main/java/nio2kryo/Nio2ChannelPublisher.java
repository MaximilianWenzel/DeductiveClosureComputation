package nio2kryo;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

import com.esotericsoftware.kryo.io.ByteBufferInput;

/**
 * A {@link Publisher} that reads data from {@link AsynchronousSocketChannel},
 * deserializes them using KRYO, and emits the resulting messages.
 * 
 * @author Yevgeny Kazakov
 *
 * @param <T>
 *            the published item type
 * 
 * @see Nio2ChannelSubscriber
 */
public class Nio2ChannelPublisher<T extends Serializable> implements
		Flow.Publisher<T>, Flow.Subscription, CompletionHandler<Integer, Void> {

	/**
	 * The channel from which data is read.
	 */
	private final AsynchronousSocketChannel channel_;
	/**
	 * The buffer into which the data is written from the channel and from which
	 * it is deserialized.
	 */
	private final ByteBufferInput kryoInput_;
	/**
	 * How many bytes is yet to read from the socket to the buffer before
	 * deserialization can start.
	 */
	private int toReadBytes_ = 0;
	/**
	 * Manages downstream subscribers to this publisher.
	 */
	private final Flow.Processor<T, T> subscriberManager_;

	/**
	 * Creates a {@link Publisher} that reads data from
	 * {@link AsynchronousSocketChannel} using the buffer size which is
	 * sufficient to deserialize the data.
	 * 
	 * @param channel
	 *            the socket channel from which to read the data
	 * 
	 */
	public Nio2ChannelPublisher(AsynchronousSocketChannel channel) {
		this(channel, Nio2ChannelSubscriber.MAX_BUFFER_SIZE);
	}

	/**
	 * Creates a {@link Publisher} that reads data from
	 * {@link AsynchronousSocketChannel} using the byte buffer of the given
	 * size.
	 * 
	 * @param channel
	 *            the socket channel from which to read the data
	 * 
	 * @param bufferSize
	 *            the number of bytes used for storing the data before it can be
	 *            deserialized; this should be at least the size of the buffer
	 *            used for writing the data to the channel!
	 */
	public Nio2ChannelPublisher(AsynchronousSocketChannel channel,
			int bufferSize) {
		this.channel_ = channel;
		ByteBuffer buf = ByteBuffer.allocateDirect(bufferSize);
		buf.flip(); // switch to read mode
		kryoInput_ = new ByteBufferInput(buf);
		subscriberManager_ = new MulticastProcessor<>();
		subscriberManager_.onSubscribe(this);
	}

	@Override
	public void subscribe(Subscriber<? super T> subscriber) {
		subscriberManager_.subscribe(subscriber);
	}

	/**
	 * The number of items currently requested but not yet published to the
	 * subscribers.
	 */
	private long requested_ = 0;

	@Override
	public void request(long n) {
		requested_ += n;
		readItems();
	}

	@Override
	public void cancel() {
		requested_ = 0;
	}

	/**
	 * A flag to indicate that the messages are currently being read from the
	 * buffer. This is to avoid a potential stack overflow due to recursive call
	 * of {@link #readItems()}.
	 */
	private boolean readingItems_ = false;

	@SuppressWarnings("unchecked")
	private void readItems() {
		if (readingItems_) {
			return; // already reading in a (recursive) call
		}
		try {
			readingItems_ = true;
			while (requested_ > 0) {
				if (toReadBytes_ == 0) {
					if (kryoInput_.limit() - kryoInput_
							.position() < Nio2ChannelSubscriber.SIZE_BYTES) {
						pull();
						if (pendingRead_) {
							return; // continue from the callback
						}
					}
					toReadBytes_ = kryoInput_.readShortUnsigned();
				}
				if (kryoInput_.limit() - kryoInput_.position() < toReadBytes_) {
					pull();
					if (pendingRead_) {
						return; // continue from the callback
					}
				}
				toReadBytes_ += kryoInput_.position();
				Object object = KryoConfig.get().readClassAndObject(kryoInput_);
				toReadBytes_ -= kryoInput_.position();
				subscriberManager_.onNext((T) object); // may call readItems()
				requested_--;
			}
		} finally {
			readingItems_ = false;
		}
	}

	/**
	 * The flag indicating that there is a pending read operation from the
	 * channel
	 */
	private boolean pendingRead_ = false;

	/**
	 * Reads the data from the channel to the buffer.
	 */
	private void pull() {
		if (pendingRead_) {
			throw new IllegalStateException(
					"The previous channel read operation did not complete yet!");
		}
		ByteBuffer buf = kryoInput_.getByteBuffer();
		buf.compact();
		if (toReadBytes_ > buf.capacity()) {
			throw new RuntimeException("No space left in buffer!");
		}
		pendingRead_ = true;
		channel_.read(buf, null, this); // may complete immediately
	}

	@Override
	public void completed(Integer result, Void attachment) {
		pendingRead_ = false;
		ByteBuffer buf = kryoInput_.getByteBuffer();
		buf.flip();
		kryoInput_.setBuffer(buf);
		readItems();
	}

	@Override
	public void failed(Throwable exc, Void attachment) {
		// TODO: handle
		exc.printStackTrace();
	}

}