package eldlsyntax;

import java.util.Objects;

/**
 * The concept ⊤ representing the all objects of the modeled domain
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public class ELConceptTop extends ELConcept {

    @Override
    public int hashCode() {
        return Objects.hash(ELConceptTop.class);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        // else
        if (obj instanceof ELConceptTop) {
            return true;
        }
        // else
        return false;
    }

    @Override
    public String toString() {
        return "⊤";
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}
