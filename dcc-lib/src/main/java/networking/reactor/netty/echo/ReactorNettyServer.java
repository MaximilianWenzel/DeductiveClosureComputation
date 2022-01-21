package networking.react.netty.echo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import reactor.netty.DisposableServer;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpServer;

public class ReactorNettyServer {

	public final static int PORT = 6748;

	public static Logger LOGGER = LoggerFactory
			.getLogger(ReactorNettyServer.class);

	public static void main(String[] args) {

		LoopResources loop = LoopResources.create("event-loop", 1, true);

		DisposableServer server = TcpServer.create().runOn(loop).port(PORT)
				// some options can be set, not sure what they do
				.childOption(ChannelOption.ALLOCATOR,
						PooledByteBufAllocator.DEFAULT)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.doOnChannelInit((observer, channel, remoteAddress) -> {
					channel.pipeline().addFirst(new NettyKryoDecoder(),
							new NettyKryoEncoder());
				}).handle((inbound, outbound) -> outbound
						.sendObject(inbound.receiveObject()))
				.bindNow();

		server.onDispose().block();

	}

}
