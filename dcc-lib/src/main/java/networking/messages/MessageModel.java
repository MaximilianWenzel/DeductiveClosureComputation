package networking.messages;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public abstract class MessageModel implements Serializable {

    private static final AtomicLong messageIDCounter = new AtomicLong(1);
    protected long senderID;
    protected long messageID = messageIDCounter.getAndIncrement();

    public MessageModel(long senderID) {
        this.senderID = senderID;
    }

    public long getSenderID() {
        return senderID;
    }

    public abstract void accept(MessageModelVisitor visitor);

    public long getMessageID() {
        return messageID;
    }
}
