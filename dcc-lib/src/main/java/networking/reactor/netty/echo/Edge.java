package networking.reactor.netty.echo;

/**
 * Simply a pair of integers
 * 
 * @author Yevgeny Kazakov
 */
public final class Edge implements Axiom {

	/**
	 * 
	 */
	private static final long serialVersionUID = -504271189081157024L;

	private int from_, to_;

	public Edge(int first, int second) {
		this.from_ = first;
		this.to_ = second;
	}

	public Edge() {

	}

	@Override
	public int hashCode() {
		return from_ + to_;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof Edge) {
			Edge other = (Edge) obj;
			return other.from_ == from_ && other.to_ == to_;
		}
		return false;
	}

	@Override
	public String toString() {
		return "Edge(" + from_ + ", " + to_ + ")";
	}

}
