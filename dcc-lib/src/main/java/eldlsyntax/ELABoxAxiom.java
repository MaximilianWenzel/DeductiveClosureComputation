package eldlsyntax;

/**
 * A restriction that involves individuals
 * 
  * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public abstract class ELABoxAxiom extends ELAxiom {

	/**
	 * The visitor pattern for ABox axiom types
	 * 
	  * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
	 */
	public interface Visitor {

		void visit(ELConceptAssertion axiom);

		void visit(ELRoleAssertion axiom);

	}

	public abstract void accept(Visitor visitor);

	@Override
	public void accept(ELAxiom.Visitor visitor) {
		accept((Visitor) visitor);
	}

}
