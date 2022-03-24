package benchmark.graphgeneration;

import benchmark.transitiveclosure.ToldReachability;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ReachabilityRandomDigraphGenerator extends RandomDigraph<ToldReachability> {
    protected Map<Integer, Integer> nodeIDToSortedRankMap = new HashMap<>();

    public ReachabilityRandomDigraphGenerator(int numberOfNodes, double probabilityForEdgeBetweenTwoNodes) {
        super(numberOfNodes, probabilityForEdgeBetweenTwoNodes);
    }

    @Override
    public List<ToldReachability> generateGraph() {
        List<ToldReachability> edges = super.generateGraph();

        List<Integer> nodeIDsSortedByCardinality = new ArrayList<>(numberOfNodes);
        nodeIDToNumberOfOccurrences.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(AtomicInteger::get)))
                .forEachOrdered(entry -> {
                    nodeIDsSortedByCardinality.add(entry.getKey());
                });

        int index = 1;
        for (Integer nodeID : nodeIDsSortedByCardinality) {
            nodeIDToSortedRankMap.put(nodeID, index++);
        }

        // replace previous node IDs by their rank
        edges.forEach(edge -> {
            edge.setSourceNode(nodeIDToSortedRankMap.get(edge.getSourceNode()));
            edge.setDestinationNode(nodeIDToSortedRankMap.get(edge.getDestinationNode()));
        });

        return edges;
    }

    @Override
    protected ToldReachability generateEdge(int sourceNode, int destinationNode) {
        return new ToldReachability(sourceNode, destinationNode);
    }

    @Override
    public String getGraphTypeName() {
        return "RandomDigraph";
    }
}
