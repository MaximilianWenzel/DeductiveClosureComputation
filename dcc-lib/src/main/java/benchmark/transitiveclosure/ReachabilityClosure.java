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
    public boolean add(Reachability reachability) {
        RoaringBitmap nodesReachableFromX;
        if (reachability instanceof DerivedReachability) {
            RoaringBitmap nodesWithConnectionToY = nodeToIncomingConnectedNodesMap.computeIfAbsent(
                    reachability.getDestinationNode(), p -> new RoaringBitmap());
            nodesWithConnectionToY.add(reachability.getSourceNode());
            nodesReachableFromX = nodeToOutgoingConnectedNodesMap.computeIfAbsent(reachability.getSourceNode(),
                    p -> new RoaringBitmap());

        } else if (reachability instanceof ToldReachability) {
            nodesReachableFromX = toldNodeToOutgoingConnectedNodesMap.computeIfAbsent(reachability.getSourceNode(),
                    p -> new RoaringBitmap());
        } else {
            throw new IllegalArgumentException();
        }
        boolean isNewValue = !nodesReachableFromX.contains(reachability.getDestinationNode());
        nodesReachableFromX.add(reachability.getDestinationNode());
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
    public boolean contains(Reachability reachability) {
        RoaringBitmap destinationNodes;
        if (reachability instanceof ToldReachability) {
            destinationNodes = this.toldNodeToOutgoingConnectedNodesMap.getOrDefault(reachability.getSourceNode(),
                    emptyRoaringBitmap);
        } else {
            destinationNodes = this.nodeToOutgoingConnectedNodesMap.getOrDefault(reachability.getSourceNode(),
                    emptyRoaringBitmap);
        }
        return destinationNodes.contains(reachability.getDestinationNode());
    }

    @Override
    public boolean remove(Reachability reachability) {
        RoaringBitmap connectedNodeIDs = nodeToOutgoingConnectedNodesMap.get(reachability.getSourceNode());
        if (connectedNodeIDs != null) {
            boolean containedValue = connectedNodeIDs.contains(reachability.getDestinationNode());
            connectedNodeIDs.remove(reachability.getDestinationNode());
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
