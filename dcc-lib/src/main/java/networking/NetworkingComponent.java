package networking;

import networking.connectors.ConnectionModel;

import java.io.IOException;
import java.io.Serializable;

public interface NetworkingComponent {
    void listenOnPort(ConnectionModel portListener) throws IOException;

    void connectToServer(ConnectionModel serverConnector) throws IOException;

    /**
     * Returns whether the message could be sent.
     */
    boolean sendMessage(long socketID, Serializable message);

    void terminate();

    boolean socketsCurrentlyReadMessages();

    void closeSocket(long socketID);

    void closeAllSockets();

    void closeServerSockets();
}
