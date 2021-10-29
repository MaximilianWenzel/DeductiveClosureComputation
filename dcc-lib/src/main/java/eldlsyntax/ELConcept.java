package eldlsyntax;

/**
 * A set of objects of the modeled domain.
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public abstract class ELConcept extends ELObject {

    /**
     * The visitor pattern for concept types
     *
     * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
     */
    public interface Visitor {

        void visit(ELConceptBottom concept);

        void visit(ELConceptConjunction concept);

        void visit(ELConceptExistentialRestriction concept);

        void visit(ELConceptName concept);

        void visit(ELConceptTop concept);

    }

    public abstract void accept(Visitor visitor);

    @Override
    public void accept(ELObject.Visitor visitor) {
        accept((Visitor) visitor);
    }

}