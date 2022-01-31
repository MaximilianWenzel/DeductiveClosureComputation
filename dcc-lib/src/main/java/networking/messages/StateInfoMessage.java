package networking.messages;

import enums.SaturationStatusMessage;

public class StateInfoMessage extends MessageModel {

    protected SaturationStatusMessage state;

    protected StateInfoMessage() {

    }

    public StateInfoMessage(long senderID, SaturationStatusMessage state) {
        super(senderID);
        this.state = state;
    }

    public SaturationStatusMessage getStatusMessage() {
        return state;
    }

    @Override
    public void accept(MessageModelVisitor visitor) {
        visitor.visit(this);

    }

    @Override
    public String toString() {
        return "StateInfoMessage{" +
                "state=" + state +
                '}';
    }
}
