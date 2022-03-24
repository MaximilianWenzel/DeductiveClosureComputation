package networking;

import networking.connectors.ConnectionModel;

import java.io.IOException;
import java.io.Serializable;

/**
 * This interface provides an abstraction from individual network socket connections and can be used in order to connect to remote servers,
 * to open new server sockets, or to send messages to already established connections.
 */
public interface NetworkingComponent {
    void listenOnPort(ConnectionModel portListener) throws IOException;

    void connectToServer(ConnectionModel serverConnector) throws IOException;

    /**
     * Returns whether the message could be successfully sent.
     */
    boolean sendMessage(long socketID, Serializable message);

    void terminate();

    boolean socketsCurrentlyReadMessages();

    void closeSocket(long socketID);

    void closeAllSockets();

    void closeServerSockets();
}
