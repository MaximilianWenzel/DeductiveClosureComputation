package networking.messages;

public class DebugMessage extends MessageModel {

    private String message;

    public DebugMessage(long sequenceNumber, long senderID, String message) {
        super(sequenceNumber, senderID);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void accept(MessageModelVisitor visitor) {
        visitor.visit(this);
    }
}
