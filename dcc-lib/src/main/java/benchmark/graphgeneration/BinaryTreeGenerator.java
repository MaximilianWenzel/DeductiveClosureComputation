package benchmark.graphgeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BinaryTreeGenerator<E> extends GraphGenerator<E> {

    /*
    d: depth
    # edges: 2^d - 2
    # transitive edges: (d - 3) * 2^d + 4
     */

    protected final AtomicInteger nodeIDCounter = new AtomicInteger(1);
    protected List<E> edges;

    public BinaryTreeGenerator(int depth) {
        this.diameter = depth;
        this.numberOfEdgesOriginalGraph = this.getNumberOfNonTransitiveEdges(depth);
        this.numberOfEdgesInTransitiveClosure = this.getNumberOfTransitiveEdges(depth);
        this.totalNumberOfEdges = this.numberOfEdgesOriginalGraph + this.numberOfEdgesInTransitiveClosure;
    }

    @Override
    public List<E> generateGraph() {
        edges = new ArrayList<>(this.totalNumberOfEdges);
        generateGraphRec(1, nodeIDCounter.getAndIncrement());
        return edges;
    }

    private void generateGraphRec(int currentDepth, int currentNode) {
        if (currentDepth == this.diameter) {
            return;
        }

        int nextNode1 = nodeIDCounter.getAndIncrement();
        int nextNode2 = nodeIDCounter.getAndIncrement();

        edges.add(generateEdge(currentNode, nextNode1));
        generateGraphRec(currentDepth + 1, nextNode1);

        edges.add(generateEdge(currentNode, nextNode2));
        generateGraphRec(currentDepth + 1, nextNode2);
    }

    private int getNumberOfNonTransitiveEdges(int depth) {
        return (int) (Math.pow(2, depth) - 2);
    }

    private int getNumberOfTransitiveEdges(int depth) {
        return (int) ((depth - 3) * Math.pow(2, depth) + 4);
    }

}
