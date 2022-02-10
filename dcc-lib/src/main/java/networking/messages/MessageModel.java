package networking.messages;

import data.Closure;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public abstract class MessageModel<C extends Closure<A>, A extends Serializable> implements Serializable {

    private static final AtomicLong messageIDCounter = new AtomicLong(1);
    protected long senderID;
    protected long messageID = messageIDCounter.getAndIncrement();

    protected MessageModel() {

    }

    public MessageModel(long senderID) {
        this.senderID = senderID;
    }

    public long getSenderID() {
        return senderID;
    }

    public abstract void accept(MessageModelVisitor<C, A> visitor);

    public long getMessageID() {
        return messageID;
    }
}
