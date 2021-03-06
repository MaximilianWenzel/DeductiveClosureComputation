package benchmark.graphgeneration;

import java.util.List;

/**
 * This class can be used in order to generate a directed graph.
 *
 * @param <E> Type of the edges of the resulting graph.
 */
public abstract class DirectedGraphGenerator<E> {

    protected int numberOfEdgesOriginalGraph;
    protected int numberOfEdgesInTransitiveClosure;
    protected int totalNumberOfEdges;
    protected int numberOfNodes;


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

    public int getNumberOfNodes() {
        return numberOfNodes;
    }

    public abstract String getGraphTypeName();
}
