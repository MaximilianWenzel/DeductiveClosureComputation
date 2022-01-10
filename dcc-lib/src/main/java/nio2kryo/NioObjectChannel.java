package nio2kryo;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

class NioObjectChannel implements Reader, Writer {

	// private static int BUFFER_LIMIT_ = 128;
	private static int BUFFER_LIMIT_ = 4096 * 4;

	/**
	 * number of bytes occupied by the size information
	 */
	private static final int SIZE_BYTES_ = 2; // for short

	private final SocketChannel channel_;

	private final ByteBufferInput in_;
	private final ByteBufferOutput out_;
	int startWritePos = 0;
	int toReadBytes = 0;

	NioObjectChannel(SocketChannel channel) {
		ByteBuffer buf = ByteBuffer.allocateDirect(2 * BUFFER_LIMIT_);
		buf.flip();
		this.in_ = new ByteBufferInput(buf);
		buf = ByteBuffer.allocateDirect(2 * BUFFER_LIMIT_);
		out_ = new ByteBufferOutput(buf);
		this.channel_ = channel;
		setStartWritePos();
	}

	/**
	 * Read new data from the channel
	 * 
	 * @return the number of bytes that can be yet read to this buffer or -1 if
	 *         the channel has reach the end stream
	 * @throws IOException
	 */
	int readFromChannel() throws IOException {
		ByteBuffer buf = in_.getByteBuffer();
		buf.compact();
		int read = channel_.read(buf);
		int result = read < 0 ? read : buf.remaining();
		buf.flip();
		in_.setBuffer(buf);
		return result;
	}

	/**
	 * Write the pending data to the channel
	 * 
	 * @return the number of bytes remained to be written
	 * @throws IOException
	 */
	int writeToChannel() throws IOException {
		int end = out_.position();
		out_.setPosition(startWritePos);
		int writtenBytes = end - startWritePos - SIZE_BYTES_;
		if (writtenBytes > 0) {
			// write how many bytes was written
			out_.writeShort(writtenBytes);
			out_.setPosition(end);
		}
		ByteBuffer buf = out_.getByteBuffer();
		buf.flip();
		channel_.write(buf);
		buf.compact();
		out_.setBuffer(buf);
		int result = buf.position();
		setStartWritePos();
		return result;
	}

	private void setStartWritePos() {
		startWritePos = out_.position();
		out_.setPosition(startWritePos + SIZE_BYTES_);
	}

	@Override
	public String toString() {
		return NioObjectChannel.class + "input: " + in_.getByteBuffer()
				+ " output: " + out_.getByteBuffer();
	}

	@Override
	public boolean canWrite() {
		return out_.position() < BUFFER_LIMIT_;
	}

	@Override
	public boolean canRead() {
		while (toReadBytes == 0) {
			if (in_.limit() - in_.position() < SIZE_BYTES_) {
				return false;
			}
			toReadBytes = in_.readShortUnsigned();
		}
		if (in_.limit() - in_.position() < toReadBytes) {
			// System.out.println("Not enough bytes!");
			return false;
		}
		return true;
	}

	@Override
	public void write(Object o) {
		if (!canWrite()) {
			throw new RuntimeException("Cannot write!");
		}
		KryoConfig.get().writeClassAndObject(out_, o);
	}

	@Override
	public Object read() {
		if (!canRead()) {
			throw new RuntimeException("Cannot read!");
		}
		toReadBytes += in_.position();
		Object object = KryoConfig.get().readClassAndObject(in_);
		toReadBytes -= in_.position();
		return object;
	}

}