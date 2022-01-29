package networking;

import networking.connectors.ConnectionEstablishmentListener;

import java.io.IOException;

public interface NetworkingComponent {
    void listenToPort(ConnectionEstablishmentListener portListener) throws IOException;

    void connectToServer(ConnectionEstablishmentListener serverConnector) throws IOException;

    void sendMessage(long socketID, Object message);

    void terminate();

    boolean socketsCurrentlyReadMessages();

    void closeSocket(long socketID);

    void closeAllSockets();

    void terminateAfterAllMessagesHaveBeenSent();
}
