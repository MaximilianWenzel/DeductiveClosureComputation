package eldlsyntax;

/**
 * An elementary binary relation o objects of the modeled domain uniquely
 * associated with a given name.
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public class ELRoleName extends ELRole implements ELEntity {

	/**
	 * The name of this role
	 */
	private final String name_;

	/**
	 * Creates a new role with the given name
	 * 
	 * @param name
	 */
	public ELRoleName(String name) {
		this.name_ = name;
	}

	@Override
	public String getName() {
		return this.name_;
	}

	@Override
	public int hashCode() {
		return ELEntity.hashCode(this);
	}

	@Override
	public boolean equals(Object obj) {
		return ELEntity.equals(this, obj);
	}

	@Override
	public String toString() {
		return ELEntity.toString(this);
	}

	@Override
	public void accept(ELEntity.Visitor visitor) {
		visitor.visit(this);

	}

	@Override
	public void accept(ELRole.Visitor visitor) {
		visitor.visit(this);
	}
}
