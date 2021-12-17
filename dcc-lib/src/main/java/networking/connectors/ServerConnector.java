package networking.connectors;

import networking.ServerData;
import networking.io.MessageHandler;

public abstract class ServerConnector implements ConnectionEstablishmentListener {

    private ServerData serverData;
    private MessageHandler messageHandler;

    public ServerConnector(ServerData serverData, MessageHandler messageHandler) {
        this.serverData = serverData;
        this.messageHandler = messageHandler;
    }

    public ServerData getServerData() {
        return serverData;
    }

    public MessageHandler getMessageProcessor() {
        return messageHandler;
    }
}
