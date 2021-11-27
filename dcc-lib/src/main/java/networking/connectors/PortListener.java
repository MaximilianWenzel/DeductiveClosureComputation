package networking.connectors;

import java.util.Objects;

public abstract class PortListener implements ConnectionEstablishmentListener {

    private int port;

    public PortListener(int port) {
        this.port = port;
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
}
