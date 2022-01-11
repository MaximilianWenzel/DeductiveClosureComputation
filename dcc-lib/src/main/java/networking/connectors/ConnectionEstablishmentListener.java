package networking.connectors;

import networking.ServerData;
import networking.io.MessageHandler;
import networking.io.SocketManager;

import java.util.Objects;

public abstract class ConnectionEstablishmentListener {

    private ServerData serverData;
    private MessageHandler messageHandler;

    public ConnectionEstablishmentListener(ServerData portToListenOrServerToConnect, MessageHandler messageHandler) {
        this.serverData = portToListenOrServerToConnect;
        this.messageHandler = messageHandler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionEstablishmentListener that = (ConnectionEstablishmentListener) o;
        return serverData.equals(that.serverData) && Objects.equals(messageHandler, that.messageHandler);
    }


    public MessageHandler getMessageProcessor() {
        return messageHandler;
    }

    public ServerData getServerData() {
        return serverData;
    }

    public abstract void onConnectionEstablished(SocketManager socketManager);
}
