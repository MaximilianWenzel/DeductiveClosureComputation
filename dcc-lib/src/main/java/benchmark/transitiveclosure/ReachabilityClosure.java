package benchmark.transitiveclosure;

import data.Closure;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import java.io.Serializable;
import java.util.*;

public class ReachabilityClosure implements Closure<Reachability>, Serializable {

    private final HashMap<Integer, RoaringBitmap> nodeToOutgoingConnectedNodesMap = new HashMap<>();
    private final HashMap<Integer, RoaringBitmap> nodeToIncomingConnectedNodesMap = new HashMap<>();
    private final HashMap<Integer, RoaringBitmap> toldNodeToOutgoingConnectedNodesMap = new HashMap<>();
    private final RoaringBitmap emptyRoaringBitmap = new RoaringBitmap();

    @Override
    public boolean add(Reachability axiom) {
        RoaringBitmap nodesReachableFromX;
        if (axiom instanceof DerivedReachability) {
            RoaringBitmap nodesWithConnectionToY = nodeToIncomingConnectedNodesMap.computeIfAbsent(
                    axiom.getDestinationNode(), p -> new RoaringBitmap());
            nodesWithConnectionToY.add(axiom.getSourceNode());
            nodesReachableFromX = nodeToOutgoingConnectedNodesMap.computeIfAbsent(axiom.getSourceNode(),
                    p -> new RoaringBitmap());

        } else if (axiom instanceof ToldReachability) {
            nodesReachableFromX = toldNodeToOutgoingConnectedNodesMap.computeIfAbsent(axiom.getSourceNode(),
                    p -> new RoaringBitmap());
        } else {
            throw new IllegalArgumentException();
        }
        boolean isNewValue = !nodesReachableFromX.contains(axiom.getDestinationNode());
        nodesReachableFromX.add(axiom.getDestinationNode());
        return isNewValue;
    }

    public void addDerivedReachability(ToldReachability toldReachability) {
        RoaringBitmap connectedNodeIDs = nodeToOutgoingConnectedNodesMap.computeIfAbsent(
                toldReachability.getSourceNode(), p -> new RoaringBitmap());
        connectedNodeIDs.add(toldReachability.getDestinationNode());
    }

    public RoaringBitmap getDerivedOutgoingConnectedNodes(int x) {
        return this.nodeToOutgoingConnectedNodesMap.getOrDefault(x, emptyRoaringBitmap);
    }

    public RoaringBitmap getDerivedIncomingConnectedNodes(int y) {
        return this.nodeToIncomingConnectedNodesMap.getOrDefault(y, emptyRoaringBitmap);
    }

    public RoaringBitmap getToldOutgoingConnectedNodes(int y) {
        return this.toldNodeToOutgoingConnectedNodesMap.getOrDefault(y, emptyRoaringBitmap);
    }

    @Override
    public boolean addAll(List<Reachability> axioms) {
        int sizeBefore = nodeToOutgoingConnectedNodesMap.size();
        for (Reachability r : axioms) {
            this.add(r);
        }
        return sizeBefore != this.nodeToOutgoingConnectedNodesMap.size();
    }

    @Override
    public boolean contains(Reachability axiom) {
        RoaringBitmap destinationNodes;
        if (axiom instanceof ToldReachability) {
            destinationNodes = this.toldNodeToOutgoingConnectedNodesMap.getOrDefault(axiom.getSourceNode(),
                    emptyRoaringBitmap);
        } else {
            destinationNodes = this.nodeToOutgoingConnectedNodesMap.getOrDefault(axiom.getSourceNode(),
                    emptyRoaringBitmap);
        }
        return destinationNodes.contains(axiom.getDestinationNode());
    }

    @Override
    public boolean remove(Reachability axiom) {
        RoaringBitmap connectedNodeIDs = nodeToOutgoingConnectedNodesMap.get(axiom.getSourceNode());
        if (connectedNodeIDs != null) {
            boolean containedValue = connectedNodeIDs.contains(axiom.getDestinationNode());
            connectedNodeIDs.remove(axiom.getDestinationNode());
            return containedValue;
        }
        return false;
    }

    @Override
    public Collection<Reachability> getClosureResults() {
        List<Reachability> result = new ArrayList<>(nodeToOutgoingConnectedNodesMap.size());
        HashMap<Integer, RoaringBitmap> adjacencyMap;

        adjacencyMap = this.toldNodeToOutgoingConnectedNodesMap;
        for (Map.Entry<Integer, RoaringBitmap> entry : adjacencyMap.entrySet()) {
            RoaringBitmap connectedNodeIDs = entry.getValue();
            connectedNodeIDs.forEach(new IntConsumer() {
                @Override
                public void accept(int i) {
                    result.add(new ToldReachability(entry.getKey(), i));
                }
            });
        }

        adjacencyMap = this.nodeToOutgoingConnectedNodesMap;
        for (Map.Entry<Integer, RoaringBitmap> entry : adjacencyMap.entrySet()) {
            RoaringBitmap connectedNodeIDs = entry.getValue();
            connectedNodeIDs.forEach(new IntConsumer() {
                @Override
                public void accept(int i) {
                    result.add(new DerivedReachability(entry.getKey(), i));
                }
            });
        }
        return result;
    }
}
