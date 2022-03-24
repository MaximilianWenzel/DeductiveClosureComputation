package benchmark.graphgeneration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ChainGraphGenerator<E extends Serializable> extends DirectedGraphGenerator<E> {

    protected ArrayList<Integer> nodeIDs;
    protected List<E> edges;

    public ChainGraphGenerator(int depth) {
        this.diameter = depth;
        this.numberOfEdgesOriginalGraph = computeNumberOfNonTransitiveEdges(depth);
        this.numberOfEdgesInTransitiveClosure = computeNumberOfTransitiveEdges(depth);
        this.totalNumberOfEdges = this.numberOfEdgesOriginalGraph + this.numberOfEdgesInTransitiveClosure;
        this.numberOfNodes = computeNumberOfNodes(depth);

        this.nodeIDs = new ArrayList<>();
        for (int i = 0; i < numberOfNodes; i++) {
            nodeIDs.add(i);
        }
        // ensure random distributed node IDs
        Collections.shuffle(nodeIDs);
    }

    @Override
    public List<E> generateGraph() {
        edges = new ArrayList<>(totalNumberOfEdges);
        for (int i = 0; i < nodeIDs.size() - 1; i++) {
            edges.add(generateEdge(nodeIDs.get(i), nodeIDs.get(i + 1)));
        }
        return edges;
    }


    @Override
    public String getGraphTypeName() {
        return "Chain";
    }

    protected int computeNumberOfNonTransitiveEdges(int depth) {
        return depth - 1;
    }

    protected int computeNumberOfTransitiveEdges(int depth) {
        return (depth * depth - 3 * depth) / 2 + 1;
    }

    protected int computeNumberOfNodes(int depth) {
        return depth;
    }
}
