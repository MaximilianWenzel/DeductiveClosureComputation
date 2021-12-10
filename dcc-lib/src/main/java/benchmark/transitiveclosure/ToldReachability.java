package benchmark.transitiveclosure;

public class ToldReachability extends Reachability {


    public ToldReachability(int x, int y) {
        super(x, y);
    }

    @Override
    public String toString() {
        return "told(" + sourceNode + ", " + destinationNode + ")";
    }

}
