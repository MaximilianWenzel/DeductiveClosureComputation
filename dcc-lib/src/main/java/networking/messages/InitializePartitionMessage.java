package networking.messages;

import reasoning.rules.Rule;

import java.util.Collection;

public class InitializePartitionMessage extends MessageModel {

    private Collection<? extends Rule> rules;

    public InitializePartitionMessage(long sequenceNumber, long senderID, Collection<? extends Rule> rules) {
        super(sequenceNumber, senderID);
        this.rules = rules;
    }

    public Collection<? extends Rule> getRules() {
        return rules;
    }

    @Override
    public void accept(MessageModelVisitor visitor) {
        visitor.visit(this);
    }
}
