package eldlsyntax;

import java.util.stream.Stream;

/**
 * Helper methods for working with entities
 *
 * Implementation taken from https://github.com/ykazakov/rw19-dl and adjusted to EL++ description logic (DL).
 *
 */
public class ELEntities {

	/**
	 * @param individuals
	 * @return an individual that is not equal to any the given individuals
	 */
	public static ELIndividual getFreshIndividual(
			Stream<? extends ELIndividual> individuals) {
		return new ELIndividual(
				getFreshName(individuals.map(e -> e.getName())));
	}

	/**
	 * @param names
	 * @return a string that is not equal to any given names
	 */
	public static String getFreshName(Stream<String> names) {
		return names.reduce("", (p, s) -> unprexify(p, s));
	}

	/**
	 * @param p
	 *              the prefix that the returned string should have
	 * @param s
	 *              the string of which the returned string should not be a
	 *              prefix
	 * @return a string that has a given prefix and is not prefix of the given
	 *         string
	 */
	public static String unprexify(String p, String s) {
		if (s.startsWith(p)) {
			char newChar = 0;
			int l = p.length();
			if (s.length() > l && s.charAt(l) == newChar) {
				newChar++;
			}
			p += newChar;
		}
		return p;
	}

}
