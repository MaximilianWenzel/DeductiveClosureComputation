package networking.messages;

import java.io.Serializable;

public abstract class MessageModel implements Serializable {

    protected long senderID;

    public MessageModel(long senderID) {
        this.senderID = senderID;
    }

    public long getSenderID() {
        return senderID;
    }

    public abstract void accept(MessageModelVisitor visitor);

}
