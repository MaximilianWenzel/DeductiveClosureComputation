package benchmark;

import java.util.List;

public abstract class GraphGenerator {

    protected int numberOfEdgesOriginalGraph;
    protected int numberOfEdgesInTransitiveClosure;

    /**
     * Length of the longest shortest directed path in the graph. In case of a tree, in corresponds to the depth.
     */
    protected int diameter;

    public GraphGenerator(int numberOfEdgesOriginalGraph, int numberOfEdgesInTransitiveClosure, int diameter) {
        this.numberOfEdgesOriginalGraph = numberOfEdgesOriginalGraph;
        this.numberOfEdgesInTransitiveClosure = numberOfEdgesInTransitiveClosure;
        this.diameter = diameter;
    }

    public abstract List<Reachability> generateGraph();
}
