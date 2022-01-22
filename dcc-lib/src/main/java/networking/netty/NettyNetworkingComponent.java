package networking.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import networking.ServerData;
import reactor.netty.tcp.TcpClient;
import util.ConsoleUtils;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class NettyNetworkingComponent {

    private static final int BUFFER_SIZE = 512 << 10;

    private Logger log = ConsoleUtils.getLogger();

    private ConcurrentMap<Long, NettySocketManager> socketIDToSocketManager = new ConcurrentHashMap<>();
    private EventLoopGroup group;
    private int numberOfThreads = 1;


    public NettyNetworkingComponent(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        init();
    }

    private void init() {
        group = new NioEventLoopGroup(numberOfThreads);
    }

    public void listenToPort(ServerData serverData,
                             Consumer<NettySocketManager> onConnectionEstablished,
                             List<? extends ChannelHandler> inboundHandlersForPipeline,
                             List<? extends ChannelHandler> outboundHandlersForPipeline) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(group);
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.localAddress(new InetSocketAddress(serverData.getHostname(), serverData.getPortNumber()));
        serverBootstrap.childHandler(new DefaultChannelInitializer(
                        onConnectionEstablished,
                        inboundHandlersForPipeline,
                        outboundHandlersForPipeline))
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_RCVBUF, BUFFER_SIZE)
                .childOption(ChannelOption.SO_SNDBUF, BUFFER_SIZE);
                /*
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_LINGER, 0)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_LINGER, 0)
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)

                .childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 10*65536)
                .childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 2*65536)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

                 */


        try {
            // start server socket and wait until it has started
            ChannelFuture f = serverBootstrap.bind().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void connectToServer(ServerData serverData,
                                Consumer<NettySocketManager> onConnectionEstablished,
                                List<? extends ChannelHandler> inboundHandlersForPipeline,
                                List<? extends ChannelHandler> outboundHandlersForPipeline) {
        Bootstrap clientBootstrap = new Bootstrap();

        clientBootstrap.group(group);
        clientBootstrap.channel(NioSocketChannel.class);
        clientBootstrap.remoteAddress(new InetSocketAddress(serverData.getHostname(), serverData.getPortNumber()));
        clientBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        clientBootstrap.handler(new DefaultChannelInitializer(
                onConnectionEstablished,
                inboundHandlersForPipeline,
                outboundHandlersForPipeline)
        );
        clientBootstrap.connect();
    }

    public void sendMessage(long socketID, Serializable message) {

    }

    public void terminate() {
        group.shutdownGracefully();
    }

    public boolean socketsCurrentlyReadMessages() {
        return false;
    }

    public void closeSocket(long socketID) {

    }

    public void closeAllSockets() {

    }

    public void terminateAfterAllMessagesHaveBeenSent() {
        try {
            group.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public EventLoopGroup getGroup() {
        return group;
    }

    private class DefaultChannelInitializer extends ChannelInitializer<Channel> {

        private List<? extends ChannelHandler> outboundHandlers;
        private List<? extends ChannelHandler> inboundHandlers;
        private Consumer<NettySocketManager> onConnectionEstablished;

        public DefaultChannelInitializer(Consumer<NettySocketManager> onConnectionEstablished,
                                         List<? extends ChannelHandler> inboundHandler,
                                         List<? extends ChannelHandler> outboundHandler) {
            this.inboundHandlers = inboundHandler;
            this.outboundHandlers = outboundHandler;
            this.onConnectionEstablished = onConnectionEstablished;
        }

        protected void initChannel(Channel socketChannel) {
            // accepted connection is 'child' of server socket - therefore 'child handler'
            // add more handlers using this method
            // socketChannel.pipeline().addLast(new DummyHandler());
            log.info("New connection established: " + socketChannel.remoteAddress());

            NettySocketManager socketManager = new NettySocketManager((SocketChannel) socketChannel);
            NettyNetworkingComponent.this.socketIDToSocketManager.put(socketManager.getSocketID(), socketManager);

            socketChannel.pipeline().removeFirst();


            // outbound handler
            socketChannel.pipeline().addLast("KryoEncoder", new NettySocketManager.KryoEncoder());
            outboundHandlers.forEach(socketChannel.pipeline()::addLast);

            // inbound handler
            socketChannel.pipeline().addLast("KryoDecoder", new NettySocketManager.KryoDecoder());
            inboundHandlers.forEach(socketChannel.pipeline()::addLast);

            System.out.println(socketChannel.pipeline());

            onConnectionEstablished.accept(socketManager);
        }

    }
}
