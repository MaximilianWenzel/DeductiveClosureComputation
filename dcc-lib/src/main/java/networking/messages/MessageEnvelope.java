package networking.messages;

import java.io.Serializable;

public class MessageEnvelope implements Serializable {
    private long socketID;
    private Object message;

    MessageEnvelope() {

    }

    public MessageEnvelope(long receiverSocketID, Object message) {
        this.socketID = receiverSocketID;
        this.message = message;
    }

    public long getSocketID() {
        return socketID;
    }

    public Object getMessage() {
        return message;
    }
}
