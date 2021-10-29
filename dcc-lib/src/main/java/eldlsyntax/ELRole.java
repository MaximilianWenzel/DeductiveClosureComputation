package eldlsyntax;

/**
 * A binary relations on objects from the modeled domain.
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public abstract class ELRole extends ELObject {

	/**
	 * The visitor pattern for role types
	 * 
	  * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
	 */
	public interface Visitor {

		void visit(ELRoleName role);

	}

	public abstract void accept(Visitor visitor);

	@Override
	public void accept(ELObject.Visitor visitor) {
		accept((Visitor) visitor);
	}

}
