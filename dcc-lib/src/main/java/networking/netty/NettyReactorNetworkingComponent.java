package networking.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.SocketChannel;
import networking.ServerData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.*;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;
import util.ConsoleUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.logging.Logger;

public class NettyReactorNetworkingComponent {

    static final int BUFFER_SIZE = 512 << 10;
    static final int BATCH_SIZE = 4000;

    private Logger log = ConsoleUtils.getLogger();

    private ConcurrentMap<Long, NettySocketManager> socketIDToSocketManager = new ConcurrentHashMap<>();
    private List<DisposableServer> serverSocketChannels = new ArrayList<>();
    private LoopResources loop;

    public NettyReactorNetworkingComponent() {
        init();
    }

    private void init() {
        loop = LoopResources.create("event-loop", 1, false);
    }

    public void listenToPort(NettyConnectionModel connectionModel) {
        DefaultChannelInitializer channelInitializer = new DefaultChannelInitializer(connectionModel);
        ServerData serverData = connectionModel.getServerData();

        DisposableServer server = TcpServer.create().runOn(loop)
                .host(serverData.getHostname())
                .port(serverData.getPortNumber())
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                //.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                //        new WriteBufferWaterMark(0, (int) (BUFFER_SIZE * 0.9)))
                .childOption(ChannelOption.SO_RCVBUF, BUFFER_SIZE)
                .childOption(ChannelOption.SO_SNDBUF, BUFFER_SIZE)
                .doOnChannelInit(channelInitializer)
                .handle(new DefaultBatchHandler(connectionModel))
                .bindNow();
        this.serverSocketChannels.add(server);
    }

    public void connectToServer(NettyConnectionModel connectionModel) {
        DefaultChannelInitializer channelInitializer = new DefaultChannelInitializer(connectionModel);
        ServerData serverData = connectionModel.getServerData();
        Connection connection = TcpClient.create().runOn(loop)
                .host(serverData.getHostname())
                .port(serverData.getPortNumber())
                //.option(ChannelOption.WRITE_BUFFER_WATER_MARK,
                //        new WriteBufferWaterMark(0, (int) (BUFFER_SIZE * 0.9)))
                .option(ChannelOption.SO_RCVBUF, BUFFER_SIZE)
                .option(ChannelOption.SO_SNDBUF, BUFFER_SIZE)
                .doOnChannelInit(channelInitializer)
                .handle(new DefaultBatchHandler(connectionModel))
                .connectNow();
    }

    public void sendMessage(long socketID, Serializable message) {
        this.socketIDToSocketManager.get(socketID).sendMessage(message);
    }

    public void terminate() {
        closeAllSockets();
        this.loop.dispose();
    }

    public void closeAllSockets() {
        this.socketIDToSocketManager.values().forEach(s -> {
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        socketIDToSocketManager.clear();

        this.serverSocketChannels.forEach(s -> {
            try {
                s.channel().close().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        this.serverSocketChannels.clear();
    }

    private static class DefaultBatchHandler implements BiFunction<NettyInbound, NettyOutbound, Publisher<Void>> {
        private NettyConnectionModel connectionModel;

        public DefaultBatchHandler(NettyConnectionModel connectionModel) {
            this.connectionModel = connectionModel;
        }

        @Override
        public Publisher<Void> apply(NettyInbound nettyInbound, NettyOutbound nettyOutbound) {
            return ((Flux<List<?>>) nettyInbound.receiveObject())
                    .flatMapIterable(list -> list) // create single elements from ArrayList batch
                    .doOnNext((msg) -> {
                            connectionModel.getOnNewMessageReceived().accept(msg);
                    })
                    .then();
        }
    }

    private class DefaultChannelInitializer implements ChannelPipelineConfigurer {

        private NettyConnectionModel connectionModel;

        public DefaultChannelInitializer(NettyConnectionModel connectionModel) {
            this.connectionModel = connectionModel;
        }

        @Override
        public void onChannelInit(ConnectionObserver connectionObserver, Channel socketChannel,
                                  SocketAddress socketAddress) {
            // accepted connection is 'child' of server socket - therefore 'child handler'
            // add more handlers using this method
            // socketChannel.pipeline().addLast(new DummyHandler());
            log.info("New connection established on " + socketChannel.localAddress());

            NettySocketManager socketManager = new NettySocketManager((SocketChannel) socketChannel);
            NettyReactorNetworkingComponent.this.socketIDToSocketManager.put(socketManager.getSocketID(),
                    socketManager);

            String decoderName = "KryoDecoder";
            socketChannel.pipeline().addFirst(decoderName, new NettySocketManager.KryoDecoder());

            String encoderName = "KryoEncoder";
            socketChannel.pipeline().addLast(encoderName, new NettySocketManager.KryoEncoder());

            System.out.println(socketChannel.pipeline());

            connectionModel.getOnConnectionEstablished().accept(socketManager);
        }
    }
}
