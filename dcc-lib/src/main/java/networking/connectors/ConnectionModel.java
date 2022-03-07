package networking.connectors;

import networking.ServerData;
import networking.io.MessageHandler;
import networking.io.SocketManager;

import java.util.Objects;

/**
 * This class is used in the context of connecting to remote servers or to listening on a given port. The given message handler determines
 * how the correspondingly received messages are processed.
 */
public abstract class ConnectionModel {

    private final ServerData serverData;
    private final MessageHandler messageHandler;

    public ConnectionModel(ServerData portToListenOrServerToConnect, MessageHandler messageHandler) {
        this.serverData = portToListenOrServerToConnect;
        this.messageHandler = messageHandler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionModel that = (ConnectionModel) o;
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
