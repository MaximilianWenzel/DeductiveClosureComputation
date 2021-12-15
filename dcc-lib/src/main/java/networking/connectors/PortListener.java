package networking.connectors;

import networking.io.MessageProcessor;

import java.util.Objects;

public abstract class PortListener implements ConnectionEstablishmentListener {

    private int port;
    private MessageProcessor messageProcessor;

    public PortListener(int port, MessageProcessor messageProcessor) {
        this.port = port;
        this.messageProcessor = messageProcessor;
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

    public MessageProcessor getMessageProcessor() {
        return messageProcessor;
    }
}
