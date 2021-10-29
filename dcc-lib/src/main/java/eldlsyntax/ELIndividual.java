package eldlsyntax;

import java.util.Objects;

/**
 * An object of the modeled domain uniquely associated with a name
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 *
 */
public class ELIndividual extends ELObject implements ELEntity {

	/**
	 * The name of this individual
	 */
	private final String name_;

	/**
	 * Creates a new individual with the given name
	 * 
	 * @param name
	 */
	public ELIndividual(String name) {
		this.name_ = Objects.requireNonNull(name);
	}

	@Override
	public String getName() {
		return name_;
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

	/**
	 * The visitor pattern for individuals
	 * 
	 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
	 */
	public interface Visitor {

		void visit(ELIndividual individual);

	}

	@Override
	public void accept(ELObject.Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void accept(ELEntity.Visitor visitor) {
		visitor.visit(this);
	}

}
