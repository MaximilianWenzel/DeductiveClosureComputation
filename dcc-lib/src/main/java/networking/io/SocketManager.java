package networking.io;

import java.io.IOException;
import java.io.Serializable;

public interface SocketManager {
    public boolean sendMessage(Serializable message) throws IOException, InterruptedException;

    public boolean hasMessagesToSend();

    public void close() throws IOException;

    boolean hasMessagesToRead();

    long getSocketID();
}
