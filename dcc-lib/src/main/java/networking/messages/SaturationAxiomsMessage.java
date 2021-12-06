package networking.messages;

import data.Closure;
import org.checkerframework.checker.units.qual.C;

import java.io.Serializable;
import java.util.Collection;

public class SaturationAxiomsMessage<C extends Closure<A>, A extends Serializable, T extends Serializable> extends MessageModel<C, A, T> {

    private final Collection<A> axioms;

    public SaturationAxiomsMessage(long senderID, Collection<A> axioms) {
        super(senderID);
        this.axioms = axioms;
    }

    public Collection<A> getAxioms() {
        return axioms;
    }

    @Override
    public void accept(MessageModelVisitor<C, A, T> visitor) {
        visitor.visit(this);
    }
}
