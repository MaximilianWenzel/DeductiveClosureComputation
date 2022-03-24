package networking.reactor.netty.echo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class NettyKryoDecoder extends ByteToMessageDecoder {

	public static Logger LOGGER = LoggerFactory
			.getLogger(NettyKryoDecoder.class);

	private final Kryo kryo;
	private final Input input = new Input(8192);

	public NettyKryoDecoder(Kryo kryo) {
		this.kryo = kryo;
	}

	public NettyKryoDecoder() {
		this(KryoConfig.get());
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {

		// LOGGER.trace("Decoding: {}", in);

		if (in.readableBytes() < 4)
			return;

		in.markReaderIndex();

		int dataLength = in.readInt();

		if (in.readableBytes() < dataLength) {
			in.resetReaderIndex();
			return;
		}

		input.setInputStream(new ByteBufInputStream(in, dataLength));
		while (input.position() < dataLength) {
			Object object = kryo.readClassAndObject(input);
			out.add(object);
		}
	}
}