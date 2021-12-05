package benchmark;

import java.util.List;

public class BinaryTreeGenerator extends GraphGenerator{

    /*
    d: depth
    # edges: 2^d - 2
    # transitive edges: (d - 3) * 2^d + 4
     */

    public BinaryTreeGenerator(int numberOfEdgesOriginalGraph, int numberOfEdgesInTransitiveClosure, int depth) {
        super(numberOfEdgesOriginalGraph, numberOfEdgesInTransitiveClosure, depth);
    }

    @Override
    public List<Reachability> generateGraph() {
        return null;
    }
}
