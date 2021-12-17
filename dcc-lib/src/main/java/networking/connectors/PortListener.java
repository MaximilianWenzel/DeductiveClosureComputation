package networking.connectors;

import networking.io.MessageHandler;

import java.util.Objects;

public abstract class PortListener implements ConnectionEstablishmentListener {

    private int port;
    private MessageHandler messageHandler;

    public PortListener(int port, MessageHandler messageHandler) {
        this.port = port;
        this.messageHandler = messageHandler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortListener that = (PortListener) o;
        return port == that.port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(port);
    }

    public int getPort() {
        return port;
    }

    public MessageHandler getMessageProcessor() {
        return messageHandler;
    }
}
