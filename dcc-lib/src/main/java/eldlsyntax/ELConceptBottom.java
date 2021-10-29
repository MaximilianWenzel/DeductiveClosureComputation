package eldlsyntax;

import java.util.Objects;

/**
 * The concept ⊥ representing the empty set set of objects
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public class ELConceptBottom extends ELConcept {

    @Override
    public int hashCode() {
        return Objects.hash(ELConceptBottom.class);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        // else
        if (obj instanceof ELConceptBottom) {
            return true;
        }
        // else
        return false;
    }

    @Override
    public String toString() {
        return "⊥";
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}