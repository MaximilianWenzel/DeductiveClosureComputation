package eldlsyntax;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A collection of {@link ELEntity} objects
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 *
 */
public class ELSignature {

	/**
	 * The {@link ELConceptName} objects of this signature
	 */
	private final Set<ELConceptName> conceptNames_ = new HashSet<>();

	/**
	 * The {@link ELRoleName} objects of this signature
	 */
	private final Set<ELRoleName> roleNames_ = new HashSet<>();

	/**
	 * The {@link ELIndividual} objects of this signature
	 */
	private final Set<ELIndividual> individuals_ = new HashSet<>();

	/**
	 * @return the {@link ELConceptName} objects of this signature
	 */
	public Stream<? extends ELConceptName> conceptNames() {
		return conceptNames_.stream();
	}

	/**
	 * @return the {@link ELRoleName} objects of this signature
	 */
	public Stream<? extends ELRoleName> roleNames() {
		return roleNames_.stream();
	}

	/**
	 * @return the {@link ELIndividual} objects of this signature
	 */
	public Stream<? extends ELIndividual> individuals() {
		return individuals_.stream();
	}

	/**
	 * @return the {@link ELEntity} objects of this signature
	 */
	public Stream<? extends ELEntity> entities() {
		return Stream.concat(conceptNames(),
				Stream.concat(roleNames(), individuals()));
	}

	/**
	 * Adds the given {@link ELConceptName} to this signature if it does not
	 * already contain it
	 * 
	 * @param entity
	 */
	public void add(ELConceptName entity) {
		conceptNames_.add(entity);
	}

	/**
	 * Adds the given {@link ELRoleName} to this signature if it does not
	 * already contain it
	 * 
	 * @param entity
	 */
	public void add(ELRoleName entity) {
		roleNames_.add(entity);
	}

	/**
	 * Adds the given {@link ELIndividual} to this signature if it does not
	 * already contain it
	 * 
	 * @param entity
	 */
	public void add(ELIndividual entity) {
		individuals_.add(entity);
	}

	/**
	 * Removes the given {@link ELConceptName} from this signature if it
	 * contains it
	 * 
	 * @param entity
	 */
	public void remove(ELConceptName entity) {
		conceptNames_.remove(entity);
	}

	/**
	 * Removes the given {@link ELRoleName} from this signature if it contains
	 * it
	 * 
	 * @param entity
	 */
	public void remove(ELRoleName entity) {
		roleNames_.remove(entity);
	}

	/**
	 * Removes the given {@link ELIndividual} from this signature if it contains
	 * it
	 * 
	 * @param entity
	 */
	public void remove(ELIndividual entity) {
		individuals_.remove(entity);
	}

	/**
	 * Adds all {@link ELEntity} objects contained in the given {@link ELObject}
	 * to this signature if they already do not appear there
	 * 
	 * @param o
	 * @return the resulting signature after the addition
	 */
	public ELSignature addSymbolsOf(ELObject o) {
		o.accept(new ELSubObjectVisitor() {

			@Override
			public void visit(ELConceptName concept) {
				add(concept);
			}

			@Override
			public void visit(ELRoleName role) {
				add(role);

			}

			@Override
			public void visit(ELIndividual individual) {
				add(individual);
			}
		});
		return this;
	}

	/**
	 * Removes all {@link ELEntity} objects contained in the given
	 * {@link ELObject} from this signature if they appear there
	 * 
	 * @param o
	 * @return the resulting signature after the removal
	 */
	public ELSignature removeSymbolsOf(ELObject o) {
		o.accept(new ELSubObjectVisitor() {

			@Override
			public void visit(ELConceptName concept) {
				remove(concept);
			}

			@Override
			public void visit(ELRoleName role) {
				remove(role);

			}

			@Override
			public void visit(ELIndividual individual) {
				remove(individual);
			}
		});
		return this;
	}

	public static ELSignature getSignatureOf(ELObject o) {
		return new ELSignature().addSymbolsOf(o);
	}

}
