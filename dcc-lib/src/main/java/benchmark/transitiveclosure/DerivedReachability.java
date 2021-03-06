package benchmark.transitiveclosure;

public class DerivedReachability extends Reachability {

    protected DerivedReachability() {
    }

    public DerivedReachability(int x, int y) {
        super(x, y);
    }

    @Override
    public String toString() {
        return "derived(" + sourceNode + ", " + destinationNode + ")";
    }

}
