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

	public Nio2ChannelPublisher(AsynchronousSocketChannel channel) {
		this(channel, Nio2ChannelSubscriber.MAX_BUFFER_SIZE);
	}

	public Nio2ChannelPublisher(AsynchronousSocketChannel channel,
			int bufferSize) {
		if (bufferSize > Nio2ChannelSubscriber.MAX_BUFFER_SIZE) {
			throw new IllegalArgumentException("The buffer size cannot exceed "
					+ Nio2ChannelSubscriber.MAX_BUFFER_SIZE);
		}
		this.channel_ = channel;
		ByteBuffer buf = ByteBuffer.allocateDirect(bufferSize);
		buf.flip(); // switch to read mode
		kryoInput_ = new ByteBufferInput(buf);
		subscriberManager_ = new MulticastProcessor<T>();
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
	void readItems() {
		if (readingItems_) {
			return; // already reading in a (recursive) call
		}
		try {
			readingItems_ = true;
			while (requested_ > 0) {
				if (toReadBytes_ == 0) {
					if (kryoInput_.limit() - kryoInput_
							.position() < Nio2ChannelSubscriber.SIZE_BYTES
							&& pull()) {
						return; // asynchronous pull
					}
					toReadBytes_ = kryoInput_.readShortUnsigned();
				}
				if (kryoInput_.limit() - kryoInput_.position() < toReadBytes_
						&& pull()) {
					return; // asynchronous pull
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
	 * The flag to determine if the {@link #pull()} is asynchronous.
	 */
	private boolean asyncPull_ = false;

	/**
	 * Read the data from socket to the buffer.
	 * 
	 * @return {@code true} if this is an asynchronous call and {@code false}
	 *         otherwise
	 */
	boolean pull() {
		if (asyncPull_) {
			throw new IllegalStateException(
					"The previous pull operation did not complete yet!");
		}
		asyncPull_ = true;
		ByteBuffer buf = kryoInput_.getByteBuffer();
		buf.compact();
		if (toReadBytes_ > buf.capacity()) {
			throw new RuntimeException("No space left in buffer!");
		}
		channel_.read(buf, null, this); // may complete immediately
		return asyncPull_;
	}

	@Override
	public void completed(Integer result, Void attachment) {
		asyncPull_ = false;
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