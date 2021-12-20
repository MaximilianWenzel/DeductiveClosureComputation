package networking.messages;

public class AcknowledgementMessage extends MessageModel {

    private long acknowledgedMessageID;

    protected AcknowledgementMessage() {

    }

    public AcknowledgementMessage(long senderID, long acknowledgedMessageID) {
        super(senderID);
        this.acknowledgedMessageID = acknowledgedMessageID;
    }

    @Override
    public void accept(MessageModelVisitor visitor) {
        visitor.visit(this);
    }

    public long getAcknowledgedMessageID() {
        return acknowledgedMessageID;
    }
}
