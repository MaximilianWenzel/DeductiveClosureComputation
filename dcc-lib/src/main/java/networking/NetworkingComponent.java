package networking;

import networking.connectors.ConnectionModel;

import java.io.IOException;
import java.io.Serializable;

public interface NetworkingComponent {
    void listenOnPort(ConnectionModel portListener) throws IOException;

    void connectToServer(ConnectionModel serverConnector) throws IOException;

    void sendMessage(long socketID, Serializable message);

    void terminate();

    boolean socketsCurrentlyReadMessages();

    void closeSocket(long socketID);

    void closeAllSockets();

    void closeServerSockets();
}
