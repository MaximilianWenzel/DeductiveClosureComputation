package networking;

import networking.connectors.NIOConnectionModel;

import java.io.IOException;

public interface NetworkingComponent {
    void listenToPort(NIOConnectionModel portListener) throws IOException;

    void connectToServer(NIOConnectionModel serverConnector) throws IOException;

    void sendMessage(long socketID, Object message);

    void terminate();

    boolean socketsCurrentlyReadMessages();

    void closeSocket(long socketID);

    void closeAllSockets();

    void terminateAfterAllMessagesHaveBeenSent();
}
