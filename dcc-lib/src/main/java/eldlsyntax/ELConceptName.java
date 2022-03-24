package eldlsyntax;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * An elementary set of objects of the modeled domain uniquely associated with a
 * given name.
 * 
  * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public class ELConceptName extends ELConcept implements ELEntity {

	/**
	 * The name of this concept
	 */
	private String name_;

	/**
	 * Creates a new concept with the given name
	 * 
	 * @param name
	 */
	public ELConceptName(String name) {
		this.name_ = Objects.requireNonNull(name);
	}

	ELConceptName() {

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
	public void accept(ELConcept.Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void accept(ELEntity.Visitor visitor) {
		visitor.visit(this);

	}

	@Override
	public Stream<ELConcept> streamOfThisConceptAndAllContainedConcepts() {
		return Stream.of(this);
	}

}
