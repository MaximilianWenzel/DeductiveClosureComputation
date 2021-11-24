package networking.messages;

import java.io.Serializable;

public abstract class MessageModel implements Serializable {

    protected long sequenceNumber;
    protected long senderID;

    public MessageModel(long sequenceNumber, long senderID) {
        this.sequenceNumber = sequenceNumber;
        this.senderID = senderID;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public long getSenderID() {
        return senderID;
    }

    public abstract void accept(MessageModelVisitor visitor);

}
