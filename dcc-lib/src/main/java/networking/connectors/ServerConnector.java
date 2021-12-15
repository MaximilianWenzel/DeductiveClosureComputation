package networking.connectors;

import networking.ServerData;
import networking.io.MessageProcessor;

public abstract class ServerConnector implements ConnectionEstablishmentListener {

    private ServerData serverData;
    private MessageProcessor messageProcessor;

    public ServerConnector(ServerData serverData, MessageProcessor messageProcessor) {
        this.serverData = serverData;
        this.messageProcessor = messageProcessor;
    }

    public ServerData getServerData() {
        return serverData;
    }

    public MessageProcessor getMessageProcessor() {
        return messageProcessor;
    }
}
