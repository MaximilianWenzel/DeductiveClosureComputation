package networking.messages;

public class AxiomCount extends MessageModel {
    int stage;
    int sentAxioms;
    int receivedAxioms;

    protected AxiomCount() {
    }

    public AxiomCount(long senderID, int stage, int sentAxioms, int receivedAxioms) {
        super(senderID);
        this.stage = stage;
        this.sentAxioms = sentAxioms;
        this.receivedAxioms = receivedAxioms;
    }

    public int getStage() {
        return stage;
    }

    public int getSentAxioms() {
        return sentAxioms;
    }

    public int getReceivedAxioms() {
        return receivedAxioms;
    }

    @Override
    public void accept(MessageModelVisitor visitor) {
        visitor.visit(this);
    }
}
