package networking.messages;

import java.util.Collection;

public class SaturationAxiomsMessage extends MessageModel {

    private Collection<Object> axioms;

    public SaturationAxiomsMessage(long senderID, Collection<Object> axioms) {
        super(senderID);
        this.axioms = axioms;
    }

    public Collection<Object> getAxioms() {
        return axioms;
    }

    @Override
    public void accept(MessageModelVisitor visitor) {
        visitor.visit(this);
    }
}
