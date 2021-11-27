package networking.connectors;

import networking.ServerData;

public abstract class ServerConnector implements ConnectionEstablishmentListener {

    private ServerData serverData;

    public ServerConnector(ServerData serverData) {
        this.serverData = serverData;
    }

    public ServerData getServerData() {
        return serverData;
    }
}
