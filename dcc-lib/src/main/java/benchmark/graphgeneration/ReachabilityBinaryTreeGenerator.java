package benchmark.graphgeneration;

import benchmark.ToldReachability;

public class ReachabilityBinaryTreeGenerator extends BinaryTreeGenerator<ToldReachability> {
    public ReachabilityBinaryTreeGenerator(int depth) {
        super(depth);
    }

    @Override
    protected ToldReachability generateEdge(int sourceNode, int destinationNode) {
        return new ToldReachability(sourceNode, destinationNode);
    }
}
