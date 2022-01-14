package networking.messages;

import java.io.Serializable;

public class MessageEnvelope implements Serializable {
    private final long socketID;
    private final Serializable message;

    public MessageEnvelope(long receiverSocketID, Serializable message) {
        this.socketID = receiverSocketID;
        this.message = message;
    }

    public long getSocketID() {
        return socketID;
    }

    public Serializable getMessage() {
        return message;
    }
}
