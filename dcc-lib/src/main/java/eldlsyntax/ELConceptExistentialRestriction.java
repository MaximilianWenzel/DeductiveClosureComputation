package eldlsyntax;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * The concept ∃ R.C representing objects that are connected by R represented by
 * the given role to at least one object of C.
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public class ELConceptExistentialRestriction extends ELConcept {

    /**
     * The relation using which objects of this concept must be connected
     */
    private final ELRole relation_;

    /**
     * The concept
     *
     * containing at least one relation successor of all objects represented by
     * this concept
     */
    private final ELConcept filler_;

    /**
     * Creates a new existential restriction using the given role and concept
     *
     * @param relation
     * @param filler
     */
    public ELConceptExistentialRestriction(ELRole relation, ELConcept filler) {
        this.relation_ = Objects.requireNonNull(relation);
        this.filler_ = Objects.requireNonNull(filler);
    }

    /**
     * @return the relation using which objects of this concept must be
     *         connected
     */
    public ELRole getRelation() {
        return this.relation_;
    }

    /**
     * @return the concept containing at least one relation successor of all
     *         objects represented by this concept
     */
    public ELConcept getFiller() {
        return this.filler_;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ELConceptExistentialRestriction.class,
                relation_.hashCode(), filler_.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        // else
        if (obj instanceof ELConceptExistentialRestriction) {
            ELConceptExistentialRestriction other = (ELConceptExistentialRestriction) obj;
            return Objects.equals(relation_, other.relation_)
                    && Objects.equals(filler_, other.filler_);
        }
        // else
        return false;
    }

    @Override
    public String toString() {
        return "(∃ " + getRelation() + ". " + getFiller() + ")";
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Stream<ELConcept> streamOfThisConceptAndAllContainedConcepts() {
        return Stream.concat(Stream.of(this), this.filler_.streamOfThisConceptAndAllContainedConcepts());
    }

}