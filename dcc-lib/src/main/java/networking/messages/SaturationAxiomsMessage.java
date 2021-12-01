package networking.messages;

import java.io.Serializable;
import java.util.Collection;

public class SaturationAxiomsMessage extends MessageModel {

    private Collection<? extends Serializable> axioms;

    public SaturationAxiomsMessage(long senderID, Collection<? extends Serializable> axioms) {
        super(senderID);
        this.axioms = axioms;
    }

    public Collection<? extends Serializable> getAxioms() {
        return axioms;
    }

    @Override
    public void accept(MessageModelVisitor visitor) {
        visitor.visit(this);
    }
}
