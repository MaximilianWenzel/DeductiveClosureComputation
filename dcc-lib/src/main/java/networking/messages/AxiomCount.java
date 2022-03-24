package networking.messages;

/**
 * A message that is sent from the worker nodes to the control node which contains information concerning the number of sent and received
 * axioms. The message is required by the control node to determine if all workers converged.
 */
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

    @Override
    public String toString() {
        return "sender=" + this.senderID + ", AxiomCount{" +
                "stage=" + stage +
                ", sentAxioms=" + sentAxioms +
                ", receivedAxioms=" + receivedAxioms +
                '}';
    }
}
