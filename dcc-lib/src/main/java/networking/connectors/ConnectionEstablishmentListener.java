package networking.connectors;

import networking.SocketManager;

public interface ConnectionEstablishmentListener {

    void onConnectionEstablished(SocketManager socketManager);
}
