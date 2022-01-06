package networking.connectors;

import networking.ServerData;
import networking.io.MessageHandler;

import java.util.Objects;

public abstract class PortListener implements ConnectionEstablishmentListener {

    private ServerData serverData;
    private MessageHandler messageHandler;

    public PortListener(ServerData serverData, MessageHandler messageHandler) {
        this.serverData = serverData;
        this.messageHandler = messageHandler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortListener that = (PortListener) o;
        return serverData.equals(that.serverData) && Objects.equals(messageHandler, that.messageHandler);
    }


    public MessageHandler getMessageProcessor() {
        return messageHandler;
    }

    public ServerData getServerData() {
        return serverData;
    }
}
