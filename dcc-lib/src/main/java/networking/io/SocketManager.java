package networking.io;

import java.io.IOException;
import java.io.Serializable;

/**
 * This class represents an abstraction from a given socket connection to send messages.
 */
public interface SocketManager {
    boolean sendMessage(Serializable message);

    boolean hasMessagesToSend();

    void close() throws IOException;

    boolean hasMessagesToRead();

    long getSocketID();
}
