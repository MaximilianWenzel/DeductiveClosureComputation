package eldlsyntax;

/**
 * A restriction imposed on the modeled domain.
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public abstract class ELAxiom extends ELObject {

    /**
     * The visitor pattern for axiom types
     *
      * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
     *
     */
    public interface Visitor extends ELABoxAxiom.Visitor, ELTBoxAxiom.Visitor {

    }

    public abstract void accept(Visitor visitor);

    @Override
    public void accept(ELObject.Visitor visitor) {
        accept((Visitor) visitor);
    }

}
