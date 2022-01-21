package networking.react.netty.echo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NettyKryoEncoder extends MessageToByteEncoder<Object> {

	public static Logger LOGGER = LoggerFactory
			.getLogger(NettyKryoEncoder.class);

	private final Kryo kryo;
	private final Output output = new Output(8192, Integer.MAX_VALUE);

	public NettyKryoEncoder(Kryo kryo) {
		this.kryo = kryo;
	}

	public NettyKryoEncoder() {
		this(KryoConfig.get());
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) {
		// TODO: send objects until the buffer half full
		kryo.writeClassAndObject(output, in);
		int written = output.position();
		out.writeInt(written);
		out.writeBytes(output.getBuffer(), 0, written);
		output.reset();
	}

}