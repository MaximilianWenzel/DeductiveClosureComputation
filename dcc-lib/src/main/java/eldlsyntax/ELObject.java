package eldlsyntax;

import java.io.Serializable;

/**
 * A syntactic object used in description logics. See Table 1 of
 * <a href="https://doi.org/10.1007/978-3-030-31423-1_1">the paper</a>.
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public abstract class ELObject implements Serializable {

    /**
     * The visitor pattern for DL object types
     *
      * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
     */
    public interface Visitor extends ELAxiom.Visitor, ELConcept.Visitor,
            ELRole.Visitor, ELIndividual.Visitor {
    }

    public abstract void accept(Visitor visitor);

}