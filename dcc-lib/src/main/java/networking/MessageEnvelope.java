package networking;

public class MessageEnvelope {
    private final long socketID;
    private final Object message;

    public MessageEnvelope(long socketID, Object message) {
        this.socketID = socketID;
        this.message = message;
    }

    public long getSocketID() {
        return socketID;
    }

    public Object getMessage() {
        return message;
    }
}
