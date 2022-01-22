package networking.netty;

import io.netty.channel.ChannelHandler;
import networking.ServerData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;

public class NettyConnectionModel {

    private ServerData serverData;
    private Consumer<NettySocketManager> onConnectionEstablished;
    private Consumer<Object> onNewMessageReceived;
    private Flux<?> outboundFlux;
    private List<? extends ChannelHandler> inboundHandlersForPipeline;
    private List<? extends ChannelHandler> outboundHandlersForPipeline;

    public NettyConnectionModel(ServerData serverData,
                                Consumer<NettySocketManager> onConnectionEstablished,
                                Consumer<Object> onNewMessageReceived,
                                Flux<?> outboundFlux,
                                List<? extends ChannelHandler> inboundHandlersForPipeline,
                                List<? extends ChannelHandler> outboundHandlersForPipeline) {
        this.serverData = serverData;
        this.onConnectionEstablished = onConnectionEstablished;
        this.onNewMessageReceived = onNewMessageReceived;
        this.outboundFlux = outboundFlux;
        this.inboundHandlersForPipeline = inboundHandlersForPipeline;
        this.outboundHandlersForPipeline = outboundHandlersForPipeline;
    }

    public ServerData getServerData() {
        return serverData;
    }

    public Consumer<NettySocketManager> getOnConnectionEstablished() {
        return onConnectionEstablished;
    }

    public Consumer<Object> getOnNewMessageReceived() {
        return onNewMessageReceived;
    }

    public Flux<?> getOutboundFlux() {
        return outboundFlux;
    }

    public List<? extends ChannelHandler> getInboundHandlersForPipeline() {
        return inboundHandlersForPipeline;
    }

    public List<? extends ChannelHandler> getOutboundHandlersForPipeline() {
        return outboundHandlersForPipeline;
    }
}
