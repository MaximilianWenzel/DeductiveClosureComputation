package eldlsyntax;

import java.util.Objects;

/**
 * The axiom C ⊑ D stating that every object represented by concept C is also an
 * object represented by concept D.
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public class ELConceptInclusion extends ELTBoxAxiom {

    /**
     * The concepts involved in the construction of this axioms; all members of
     * {@link #subConcept_} must be included in {@link #superConcept_}
     */
    private final ELConcept subConcept_, superConcept_;

    /**
     * Creates a new axiom stating inclusion of objects of the given concepts
     *
     * @param subConcept
     * @param superConcept
     */
    public ELConceptInclusion(ELConcept subConcept, ELConcept superConcept) {
        this.subConcept_ = Objects.requireNonNull(subConcept);
        this.superConcept_ = Objects.requireNonNull(superConcept);
    }

    /**
     * @return the sub-concept of this axiom
     */
    public ELConcept getSubConcept() {
        return this.subConcept_;
    }

    /**
     * @return the super-concept of this axiom
     */
    public ELConcept getSuperConcept() {
        return this.superConcept_;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ELConceptInclusion.class, subConcept_.hashCode(),
                superConcept_.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        // else
        if (obj instanceof ELConceptInclusion) {
            ELConceptInclusion other = (ELConceptInclusion) obj;
            return Objects.equals(subConcept_, other.subConcept_)
                    && Objects.equals(superConcept_, other.superConcept_);
        }
        // else
        return false;
    }

    @Override
    public String toString() {
        return getSubConcept() + " ⊑ " + getSuperConcept();
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}