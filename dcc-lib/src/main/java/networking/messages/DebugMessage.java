package networking.messages;

public class DebugMessage extends MessageModel {

    private String message;

    protected DebugMessage() {
    }

    public DebugMessage(long senderID, String message) {
        super(senderID);
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
