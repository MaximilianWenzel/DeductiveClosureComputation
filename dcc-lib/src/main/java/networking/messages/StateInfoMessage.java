package networking.messages;

import enums.SaturationStatusMessage;

/**
 * This message contains information concerning the current closure computation state, e.g., in order to start the closure computation
 * procedure.
 */
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
}
