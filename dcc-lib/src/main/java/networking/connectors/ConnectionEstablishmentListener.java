package networking.connectors;

import networking.io.SocketManager;

public interface ConnectionEstablishmentListener {

    void onConnectionEstablished(SocketManager socketManager);
}
