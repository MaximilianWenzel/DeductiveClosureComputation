package eldlsyntax;

/**
 * A restriction that does not involve individuals.
  * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public abstract class ELTBoxAxiom extends ELAxiom {

    /**
     * The visitor pattern for TBox axiom types
     *
     */
    public interface Visitor {

        void visit(ELConceptInclusion axiom);
    }

    public abstract void accept(Visitor visitor);

    @Override
    public void accept(ELAxiom.Visitor visitor) {
        accept((Visitor) visitor);
    }

}
