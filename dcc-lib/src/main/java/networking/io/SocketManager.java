package networking.io;

import java.io.IOException;

public interface SocketManager {
    boolean sendMessage(Object message);

    boolean hasMessagesToSend();

    void close() throws IOException;

    boolean hasMessagesToRead();

    long getSocketID();
}
