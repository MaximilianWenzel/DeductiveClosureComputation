package networking.messages;

import java.io.Serializable;

public class MessageEnvelope implements Serializable {
    private long socketID;
    private Object message;

    public static final MessageEnvelope EMPTY = new MessageEnvelope(-1, null);

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

    @Override
    public String toString() {
        return "MessageEnvelope{" +
                "socketID=" + socketID +
                ", message=" + message +
                '}';
    }
}
