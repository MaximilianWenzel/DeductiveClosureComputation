package networking.messages;

public class RequestAxiomMessageCount extends MessageModel {

    private int stage;

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
