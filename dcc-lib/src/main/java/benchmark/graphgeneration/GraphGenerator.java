package benchmark.graphgeneration;

import java.util.List;

public abstract class GraphGenerator<E> {

    protected int numberOfEdgesOriginalGraph;
    protected int numberOfEdgesInTransitiveClosure;
    protected int totalNumberOfEdges;


    /**
     * Length of the longest shortest directed path in the graph. In case of a tree, in corresponds to the depth.
     */
    protected int diameter;

    public abstract List<E> generateGraph();

    protected abstract E generateEdge(int sourceNode, int destinationNode);

    public int getNumberOfEdgesOriginalGraph() {
        return numberOfEdgesOriginalGraph;
    }

    public int getNumberOfEdgesInTransitiveClosure() {
        return numberOfEdgesInTransitiveClosure;
    }

    public int getTotalNumberOfEdges() {
        return totalNumberOfEdges;
    }

    public int getDiameter() {
        return diameter;
    }
}
