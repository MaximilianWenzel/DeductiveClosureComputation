package eldlsyntax;

/**
 * A {@link ELObject.Visitor} that is accepted for the sub-objects of the
 * visited {@link ELObject}. Mostly useful as a prototype of recursive methods
 * over {@link ELObject}.
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 *
 */
public class ELSubObjectVisitor implements ELObject.Visitor {

	@Override
	public void visit(ELConceptAssertion axiom) {
		axiom.getInstance().accept(this);
		axiom.getType().accept(this);
	}

	@Override
	public void visit(ELRoleAssertion axiom) {
		axiom.getRelation().accept(this);
		axiom.getFirstInstance().accept(this);
		axiom.getSecondInstance().accept(this);
	}

	@Override
	public void visit(ELConceptInclusion axiom) {
		axiom.getSubConcept().accept(this);
		axiom.getSuperConcept().accept(this);
	}

	@Override
	public void visit(ELConceptBottom concept) {
		// no sub-objects
	}

	@Override
	public void visit(ELConceptConjunction concept) {
		concept.getFirstConjunct().accept(this);
		concept.getSecondConjunct().accept(this);
	}

	@Override
	public void visit(ELConceptExistentialRestriction concept) {
		concept.getRelation().accept(this);
		concept.getFiller().accept(this);
	}

	@Override
	public void visit(ELConceptName concept) {
		// no sub-objects
	}

	@Override
	public void visit(ELConceptTop concept) {
		// no sub-objects
	}

	@Override
	public void visit(ELRoleName role) {
		// no sub-objects
	}

	@Override
	public void visit(ELIndividual individual) {
		// no sub-objects
	}

}
