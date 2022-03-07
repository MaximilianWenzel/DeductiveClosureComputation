package networking.messages;

import java.io.Serializable;

/**
 * This class is used in order to assign a given object to the designated socket to which it is to be sent.
 */
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
