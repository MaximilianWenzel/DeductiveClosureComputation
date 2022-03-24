package benchmark.graphgeneration;

import benchmark.transitiveclosure.ToldReachability;

public class ReachabilityChainGraphGenerator extends ChainGraphGenerator<ToldReachability> {
    public ReachabilityChainGraphGenerator(int depth) {
        super(depth);
    }

    @Override
    protected ToldReachability generateEdge(int sourceNode, int destinationNode) {
        return new ToldReachability(sourceNode, destinationNode);
    }
}
