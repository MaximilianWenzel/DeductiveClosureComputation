package networking.messages;

/**
 * A message which can be used in order to acknowledge on the application layer another message model object.
 */
public class AcknowledgementMessage extends MessageModel {

    /**
     * Message ID of the message which is to be acknowledged.
     */
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
