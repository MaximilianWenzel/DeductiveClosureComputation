package networking.messages;

import java.io.Serializable;

public class MessageEnvelope implements Serializable {
    private long socketID;
    private Serializable message;

    MessageEnvelope() {

    }

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
