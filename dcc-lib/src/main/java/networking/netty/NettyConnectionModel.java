package networking.netty;

import io.netty.channel.ChannelHandler;
import networking.ServerData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

public abstract class NettyConnectionModel {

    private ServerData serverData;
    private List<? extends ChannelHandler> inboundHandlersForPipeline;
    private List<? extends ChannelHandler> outboundHandlersForPipeline;

    public NettyConnectionModel(ServerData serverData,
                                List<? extends ChannelHandler> inboundHandlersForPipeline,
                                List<? extends ChannelHandler> outboundHandlersForPipeline) {
        this.serverData = serverData;
        this.inboundHandlersForPipeline = inboundHandlersForPipeline;
        this.outboundHandlersForPipeline = outboundHandlersForPipeline;
    }

    public ServerData getServerData() {
        return serverData;
    }

    public List<? extends ChannelHandler> getInboundHandlersForPipeline() {
        return inboundHandlersForPipeline;
    }

    public List<? extends ChannelHandler> getOutboundHandlersForPipeline() {
        return outboundHandlersForPipeline;
    }

    public abstract void onConnectionEstablished(Flux<?> outboundFlux, Sinks.Many<Object> receivedMessagesSink);

}
