package eldlsyntax;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A collection of {@link ELAxiom} objects
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 *
 */
public class ELOntology {

	/**
	 * The {@link ELTBoxAxiom}s of this ontology
	 */
	protected final Set<ELConceptInclusion> tBox_ = new HashSet<>();

	/**
	 * The {@link ELABoxAxiom}s of this ontology
	 */
	protected final Set<ELABoxAxiom> aBox_ = new HashSet<>();

	/**
	 * @return all {@link ELTBoxAxiom}s of this ontology
	 */
	public Stream<? extends ELTBoxAxiom> tBox() {
		return tBox_.stream();
	}

	/**
	 * @return all {@link ELABoxAxiom}s of this ontology
	 */
	public Stream<? extends ELABoxAxiom> aBox() {
		return aBox_.stream();
	}

	public Stream<? extends ELAxiom> axioms() {
		return Stream.concat(tBox(), aBox());
	}

	public ELSignature getSignature() {
		ELSignature s = new ELSignature();
		axioms().forEach(ax -> s.addSymbolsOf(ax));
		return s;
	}

	/**
	 * Adds the given {@link ELTBoxAxiom} to this ontology
	 *
	 * @param axiom
	 */
	public void add(ELTBoxAxiom axiom) {
		tBox_.add((ELConceptInclusion) axiom);
	}

	/**
	 * Adds the given {@link ELABoxAxiom} to this ontology
	 *
	 * @param axiom
	 */
	public void add(ELABoxAxiom axiom) {
		aBox_.add(axiom);
	}

	/**
	 * Removes the given {@link ELTBoxAxiom} from this ontology
	 *
	 * @param axiom
	 */
	public void remove(ELTBoxAxiom axiom) {
		tBox_.remove(axiom);
	}

	/**
	 * Removes the given {@link ELABoxAxiom} from this ontology
	 *
	 * @param axiom
	 */
	public void remove(ELABoxAxiom axiom) {
		aBox_.remove(axiom);
	}

	/**
	 * Adds the given {@link ELAxiom} to this ontology
	 *
	 * @param axiom
	 * @return the resulting ontology with the added axiom
	 */
	public ELOntology add(ELAxiom axiom) {
		axiom.accept(new ELAxiom.Visitor() {

			@Override
			public void visit(ELConceptInclusion axiom) {
				add(axiom);
			}

			@Override
			public void visit(ELRoleAssertion axiom) {
				add(axiom);
			}

			@Override
			public void visit(ELConceptAssertion axiom) {
				add(axiom);
			}
		});
		return this;
	}

	/**
	 * Removes the given {@link ELAxiom} from this ontology
	 *
	 * @param axiom
	 * @return the resulting ontology after removal of the axiom
	 */
	public ELOntology remove(ELAxiom axiom) {
		axiom.accept(new ELAxiom.Visitor() {

			@Override
			public void visit(ELConceptInclusion axiom) {
				remove(axiom);
			}

			@Override
			public void visit(ELRoleAssertion axiom) {
				remove(axiom);
			}

			@Override
			public void visit(ELConceptAssertion axiom) {
				remove(axiom);
			}
		});
		return this;
	}

	public Set<ELConceptInclusion> getOntologyAxioms() {
		return tBox_;
	}

}
