package benchmark.transitiveclosure;

import java.io.Serializable;
import java.util.Objects;

public abstract class Reachability implements Serializable {
    int sourceNode, destinationNode;


    public Reachability(int sourceNode, int destinationNode) {
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
    }

    public int getSourceNode() {
        return sourceNode;
    }

    public int getDestinationNode() {
        return destinationNode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reachability that = (Reachability) o;
        return sourceNode == that.sourceNode && destinationNode == that.destinationNode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceNode, destinationNode);
    }
}
