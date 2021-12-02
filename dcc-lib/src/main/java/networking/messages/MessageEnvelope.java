package networking.messages;

public class MessageEnvelope {
    private final long socketID;
    private final Object message;

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
