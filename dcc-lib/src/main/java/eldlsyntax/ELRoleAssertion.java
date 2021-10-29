package eldlsyntax;

import java.util.Objects;

/**
 * States that two given individual (instances) are connected by the relation
 * represented by the given role (relation)
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public class ELRoleAssertion extends ELABoxAxiom {

	/**
	 * The instances of this role assertion
	 */
	private final ELIndividual firstInstance_, secondInstance_;

	/**
	 * The relation of this role assertion
	 */
	private final ELRole relation_;

	/**
	 * Creates a new axiom stating that the given individuals are connected by
	 * the given role.
	 * 
	 * @param relation
	 * @param firstInstance
	 * @param secondInstance
	 */
	public ELRoleAssertion(ELRole relation, ELIndividual firstInstance,
						   ELIndividual secondInstance) {
		this.firstInstance_ = Objects.requireNonNull(firstInstance);
		this.secondInstance_ = Objects.requireNonNull(secondInstance);
		this.relation_ = Objects.requireNonNull(relation);
	}

	/**
	 * @return the type of this concept assertion
	 */
	public ELRole getRelation() {
		return this.relation_;
	}

	/**
	 * @return the first instance of this role assertion
	 */
	public ELIndividual getFirstInstance() {
		return this.firstInstance_;
	}

	/**
	 * @return the first instance of this role assertion
	 */
	public ELIndividual getSecondInstance() {
		return this.secondInstance_;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ELRoleAssertion.class, firstInstance_.hashCode(),
				secondInstance_.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		// else
		if (obj instanceof ELRoleAssertion) {
			ELRoleAssertion other = (ELRoleAssertion) obj;
			return Objects.equals(firstInstance_, other.firstInstance_)
					&& Objects.equals(secondInstance_, other.secondInstance_);
		}
		// else
		return false;
	}

	@Override
	public String toString() {
		return getRelation() + "(" + getFirstInstance() + ", "
				+ getSecondInstance() + ")";
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

}
