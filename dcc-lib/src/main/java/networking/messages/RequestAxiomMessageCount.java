package networking.messages;

/**
 * A request message which is sent from the control node to a worker node in order to request an axiom count message.
 */
public class RequestAxiomMessageCount extends MessageModel {

    private int stage;

    protected RequestAxiomMessageCount() {
    }

    public RequestAxiomMessageCount(long senderID, int stage) {
        super(senderID);
        this.stage = stage;
    }

    @Override
    public void accept(MessageModelVisitor visitor) {
        visitor.visit(this);
    }

    public int getStage() {
        return stage;
    }
}
