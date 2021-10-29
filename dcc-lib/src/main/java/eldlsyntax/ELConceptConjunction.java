package eldlsyntax;

import java.util.Objects;

/**
 * The concept conjunction C ⊓ D representing the common objects represented by
 * C and D.
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 *
 */
public class ELConceptConjunction extends ELConcept {

    /**
     * The conjuncts from which this concept is constructed
     */
    private final ELConcept firstConjunct_, secondConjunct_;

    /**
     * Creates a new conjunction of the two given conjuncts
     *
     * @param firstConjunct
     * @param secondConjunct
     */
    public ELConceptConjunction(ELConcept firstConjunct,
                                ELConcept secondConjunct) {
        this.firstConjunct_ = Objects.requireNonNull(firstConjunct);
        this.secondConjunct_ = Objects.requireNonNull(secondConjunct);
    }

    /**
     * @return the first conjunct of this concept conjunction
     */
    public ELConcept getFirstConjunct() {
        return this.firstConjunct_;
    }

    /**
     * @return the second conjunct of this concept conjunction
     */
    public ELConcept getSecondConjunct() {
        return this.secondConjunct_;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ELConceptConjunction.class,
                firstConjunct_.hashCode(), secondConjunct_.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        // else
        if (obj instanceof ELConceptConjunction) {
            ELConceptConjunction other = (ELConceptConjunction) obj;
            return Objects.equals(firstConjunct_, other.firstConjunct_)
                    && Objects.equals(secondConjunct_, other.secondConjunct_);
        }
        // else
        return false;
    }

    @Override
    public String toString() {
        return "(" + getFirstConjunct() + " ⊓ " + getSecondConjunct() + ")";
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}
