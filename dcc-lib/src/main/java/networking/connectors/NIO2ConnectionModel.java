package networking.connectors;

import networking.ServerData;
import networking.io.SocketManager;

import java.util.Objects;

public abstract class NIO2ConnectionModel {

    private ServerData serverData;

    public NIO2ConnectionModel(ServerData portToListenOrServerToConnect) {
        this.serverData = portToListenOrServerToConnect;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NIO2ConnectionModel that = (NIO2ConnectionModel) o;
        return Objects.equals(serverData, that.serverData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverData);
    }

    public ServerData getServerData() {
        return serverData;
    }

    public abstract void onConnectionEstablished(SocketManager socketManager);
}
