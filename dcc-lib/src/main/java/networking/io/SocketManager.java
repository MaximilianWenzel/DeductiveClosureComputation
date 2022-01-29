package networking.io;

import java.io.IOException;

public interface SocketManager {
    public boolean sendMessage(Object message);

    public boolean hasMessagesToSend();

    public void close() throws IOException;

    boolean hasMessagesToRead();

    long getSocketID();
}
