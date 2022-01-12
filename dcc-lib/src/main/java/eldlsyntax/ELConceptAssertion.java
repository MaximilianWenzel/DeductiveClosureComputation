package eldlsyntax;

import java.util.Objects;

/**
 * The axiom C(a) stating that the individual (instance) corresponds to an
 * element represented by concept C (type).
 * 
  * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public class ELConceptAssertion extends ELABoxAxiom {

	/**
	 * The instance of this concept assertion
	 */
	private ELIndividual instance_;

	/**
	 * The type of this concept assertion
	 */
	private ELConcept type_;

	/**
	 * Creates a new axiom stating that the given individual is an instance of a
	 * given concept.
	 * 
	 * @param type
	 * @param instance
	 */
	public ELConceptAssertion(ELConcept type, ELIndividual instance) {
		this.instance_ = Objects.requireNonNull(instance);
		this.type_ = Objects.requireNonNull(type);
	}

	ELConceptAssertion() {

	}

	/**
	 * @return the type of this concept assertion
	 */
	public ELConcept getType() {
		return this.type_;
	}

	/**
	 * @return the instance of this concept assertion
	 */
	public ELIndividual getInstance() {
		return this.instance_;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ELConceptAssertion.class, type_.hashCode(),
				instance_.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		// else
		if (obj instanceof ELConceptAssertion) {
			ELConceptAssertion other = (ELConceptAssertion) obj;
			return Objects.equals(type_, other.type_)
					&& Objects.equals(instance_, other.instance_);
		}
		// else
		return false;
	}

	@Override
	public String toString() {
		return getType() + "(" + getInstance() + ")";
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

}
