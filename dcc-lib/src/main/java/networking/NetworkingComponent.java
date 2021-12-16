package networking;

import networking.connectors.PortListener;
import networking.connectors.ServerConnector;

import java.io.IOException;
import java.io.Serializable;

public interface NetworkingComponent {
    void listenToPort(PortListener portListener) throws IOException;

    void connectToServer(ServerConnector serverConnector) throws IOException;

    void sendMessage(long socketID, Serializable message);

    void terminate();

    boolean socketsCurrentlyReadMessages();
}
