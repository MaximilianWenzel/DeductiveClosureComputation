package nio2kryo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

class Nio2ObjectChannel implements Reader, Writer {

	// private static int BUFFER_LIMIT_ = 128;
	private static int BUFFER_LIMIT_ = 4096 * 8;

	/**
	 * number of bytes occupied by the size information
	 */
	private static final int SIZE_BYTES_ = 2; // for short

	private final AsynchronousSocketChannel channel_;

	private final ByteBufferInput in_;
	private final ByteBufferOutput out_;
	int startWritePos = 0;
	int toReadBytes = 0;

	boolean handleRead = false, handleWrite = false, halderStarted = false;

	Nio2ObjectChannel(AsynchronousSocketChannel channel) {
		ByteBuffer buf = ByteBuffer.allocateDirect(2 * BUFFER_LIMIT_);
		this.in_ = new ByteBufferInput(buf);
		buf = ByteBuffer.allocateDirect(2 * BUFFER_LIMIT_);
		out_ = new ByteBufferOutput(buf);
		setStartWritePos();
		this.channel_ = channel;
	}

	private final CompletionHandler<Integer, ReadAttachment<?>> readHandler = new CompletionHandler<Integer, ReadAttachment<?>>() {

		@Override
		public void completed(Integer result, ReadAttachment<?> attachment) {
			if (result < 0) {
				attachment.completed(result);
				return;
			}
			ByteBuffer buf = in_.getByteBuffer();
			buf.flip();
			in_.setBuffer(buf);
			Writer writer = attachment.writer;
			int written = 0;
			while (canRead() && writer.canWrite()) {
				writer.write(read());
				written++;
			}
			in_.getByteBuffer().compact();
			attachment.completed(written);
		}

		@Override
		public void failed(Throwable exc, ReadAttachment<?> attachment) {
			// TODO Auto-generated method stub

		}
	};

	private final CompletionHandler<Integer, WriteAttachment<?>> writeHandler = new CompletionHandler<Integer, WriteAttachment<?>>() {

		@Override
		public void completed(Integer result, WriteAttachment<?> attachment) {
			ByteBuffer buf = out_.getByteBuffer();
			int remaining = buf.remaining();
			buf.compact();
			out_.setBuffer(buf);
			setStartWritePos();
			attachment.completed(remaining == 0);
		}

		@Override
		public void failed(Throwable exc, WriteAttachment<?> attachment) {
			// TODO Auto-generated method stub

		}
	};

	private static class ReadAttachment<A> {
		final Writer writer;
		final A attachment;
		final CompletionHandler<Integer, ? super A> handler;

		ReadAttachment(Writer writer, A attachment,
				CompletionHandler<Integer, ? super A> handler) {
			this.writer = writer;
			this.attachment = attachment;
			this.handler = handler;
		}

		void completed(int result) {
			handler.completed(result, attachment);
		}
	}

	public <A> void read(Writer writer, A attachment,
			CompletionHandler<Integer, ? super A> handler) {
		ReadAttachment<A> readAttachment = new ReadAttachment<A>(writer,
				attachment, handler);
		if (canRead()) {
			readHandler.completed(0, readAttachment);
		} else {
			channel_.read(in_.getByteBuffer(), readAttachment, readHandler);
		}
	}

	private static class WriteAttachment<A> {
		final A attachment;
		final CompletionHandler<Boolean, ? super A> handler;

		WriteAttachment(int written, A attachment,
				CompletionHandler<Boolean, ? super A> handler) {
			this.attachment = attachment;
			this.handler = handler;
		}

		void completed(boolean allSent) {
			handler.completed(allSent, attachment);
		}
	}

	public <A> void write(Reader reader, A attachment,
			CompletionHandler<Boolean, ? super A> handler) {
		int written = 0;
		while (reader.canRead() && canWrite()) {
			write(reader.read());
			written++;
		}
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
		channel_.write(buf,
				new WriteAttachment<A>(written, attachment, handler),
				writeHandler);
	}

	private void setStartWritePos() {
		startWritePos = out_.position();
		out_.setPosition(startWritePos + SIZE_BYTES_);
	}

	@Override
	public String toString() {
		return Nio2ObjectChannel.class + "input: " + in_.getByteBuffer()
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

	public void close() throws IOException {
		channel_.close();
	}

}