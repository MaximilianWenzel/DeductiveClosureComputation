package benchmark.graphgeneration;

import benchmark.transitiveclosure.ToldReachability;

public class ReachabilityBinaryTreeGenerator extends BinaryTreeGenerator<ToldReachability> {
    public ReachabilityBinaryTreeGenerator(int depth) {
        super(depth);
    }

    @Override
    protected ToldReachability generateEdge(int sourceNode, int destinationNode) {
        return new ToldReachability(sourceNode, destinationNode);
    }

    @Override
    public String getGraphTypeName() {
        return "BinaryTree";
    }
}
