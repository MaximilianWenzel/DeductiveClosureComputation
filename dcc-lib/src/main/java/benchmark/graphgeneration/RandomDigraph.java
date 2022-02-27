package benchmark.graphgeneration;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class RandomDigraph<E> extends GraphGenerator<E> {
    protected List<E> edges;
    protected int numberOfNodes;
    protected double probabilityForEdgeBetweenTwoNodes;
    protected Random random = new Random();
    protected Map<Integer, AtomicInteger> nodeIDToNumberOfOccurrences = new HashMap<>();

    public RandomDigraph(int numberOfNodes, double probabilityForEdgeBetweenTwoNodes) {
        this.numberOfNodes = numberOfNodes;
        this.probabilityForEdgeBetweenTwoNodes = probabilityForEdgeBetweenTwoNodes;
        this.numberOfEdgesOriginalGraph = numberOfNodes * numberOfNodes;
    }

    @Override
    public List<E> generateGraph() {
        edges = new ArrayList<>((int) (probabilityForEdgeBetweenTwoNodes * numberOfNodes * numberOfNodes));

        for (int i = 1; i <= numberOfNodes; i++) {
            for (int j = 1; j <= numberOfNodes; j++) {
                if (Math.random() <= probabilityForEdgeBetweenTwoNodes) {
                    edges.add(generateEdge(i, j));
                    nodeIDToNumberOfOccurrences.computeIfAbsent(i, c -> new AtomicInteger(0)).incrementAndGet();
                    nodeIDToNumberOfOccurrences.computeIfAbsent(j, c -> new AtomicInteger(0)).incrementAndGet();
                }
            }
        }

        return edges;
    }
}
