package benchmark.graphgeneration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BinaryTreeGenerator<E> extends GraphGenerator<E> {

    /*
    d: depth
    # nodes: 2^(d + 1) - 1
    # edges: 2^d - 2
    # transitive edges: (d - 3) * 2^d + 4
     */

    protected final AtomicInteger nodeIndex = new AtomicInteger(0);
    protected ArrayList<Integer> nodeIDs;
    protected List<E> edges;

    public BinaryTreeGenerator(int depth) {
        this.diameter = depth;
        this.numberOfEdgesOriginalGraph = this.getNumberOfNonTransitiveEdges(depth);
        this.numberOfEdgesInTransitiveClosure = this.getNumberOfTransitiveEdges(depth);
        this.totalNumberOfEdges = this.numberOfEdgesOriginalGraph + this.numberOfEdgesInTransitiveClosure;
        this.numberOfNodes = this.getNumberOfNodes(depth);

        this.nodeIDs = new ArrayList<>();
        for (int i = 0; i < numberOfNodes; i++) {
            nodeIDs.add(i);
        }
        // ensure random distributed node IDs
        Collections.shuffle(nodeIDs);
    }

    @Override
    public List<E> generateGraph() {
        edges = new ArrayList<>(this.totalNumberOfEdges);
        generateGraphRec(1, nodeIDs.get(nodeIndex.getAndIncrement()));
        return edges;
    }

    private void generateGraphRec(int currentDepth, int currentNode) {
        if (currentDepth == this.diameter) {
            return;
        }

        int nextNode1 = nodeIDs.get(nodeIndex.getAndIncrement());
        int nextNode2 = nodeIDs.get(nodeIndex.getAndIncrement());

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

    private int getNumberOfNodes(int depth) {
        return (int) (Math.pow(2, depth + 1) - 1);
    }

}
