package networking.netty;

import networking.ServerData;
import networking.io.SocketManager;

import java.util.function.Consumer;

public class NettyConnectionModel {

    private ServerData serverData;
    private Consumer<Object> onNewMessageReceived;
    private Consumer<SocketManager> onConnectionEstablished;

    public NettyConnectionModel(ServerData serverData, Consumer<Object> onNewMessageReceived,
                                Consumer<SocketManager> onConnectionEstablished) {
        this.serverData = serverData;
        this.onNewMessageReceived = onNewMessageReceived;
        this.onConnectionEstablished = onConnectionEstablished;
    }

    public ServerData getServerData() {
        return serverData;
    }

    public Consumer<Object> getOnNewMessageReceived() {
        return onNewMessageReceived;
    }

    public Consumer<SocketManager> getOnConnectionEstablished() {
        return onConnectionEstablished;
    }
}
