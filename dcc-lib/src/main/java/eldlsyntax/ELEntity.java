package eldlsyntax;

import java.util.Objects;

/**
 * 
 * An object uniquely associated with a string name
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 */
public interface ELEntity {

	/**
	 * @return the name of this concept
	 */
	String getName();

	/**
	 * Computes the hash code of the given entity. If two entities are equal
	 * then they must have the same hash code
	 * 
	 * @param e
	 *              the entity
	 * @return the hash code of the given entity
	 * @see #equals(ELEntity, Object)
	 */
	static int hashCode(ELEntity e) {
		if (e == null) {
			return 0;
		}
		// else
		return Objects.hash(e.getClass(), e.getName());
	}

	/**
	 * Checks equality of a given entity to a given object. The equality holds
	 * if both objects are entities of the same type (e.g., they are both
	 * concept names or both role names, or both individuals) and have the same
	 * names.
	 * 
	 * @param entity
	 * @param other
	 * @return {@code true} if the two given entities are equal and
	 *         {@code false} otherwise
	 */
	static boolean equals(ELEntity entity, Object other) {
		if (entity == null) {
			return other == null;
		}
		// else
		return Objects.equals(entity.getClass(), other.getClass()) && Objects
				.equals(entity.getName(), ((ELEntity) other).getName());
	}

	/**
	 * @param entity
	 * @return the string representation of the given entity
	 */
	static String toString(ELEntity entity) {
		return entity.getName();
	}

	/**
	 * The visitor pattern for entity types
	 * 
	  * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
	 */
	interface Visitor {

		void visit(ELConceptName concept);

		void visit(ELRoleName role);

		void visit(ELIndividual individual);

	}

	void accept(Visitor visitor);

}
