package benchmark.graphgeneration;

import benchmark.transitiveclosure.Reachability;
import benchmark.transitiveclosure.ToldReachability;
import org.apache.jena.graph.Graph;

import java.util.List;

public class StaticGraphGenerator extends GraphGenerator<ToldReachability> {
    private List<ToldReachability> initialAxioms;

    public StaticGraphGenerator(List<ToldReachability> initialAxioms) {
        this.initialAxioms = initialAxioms;
    }

    @Override
    public List<ToldReachability> generateGraph() {
        return initialAxioms;
    }

    @Override
    protected ToldReachability generateEdge(int sourceNode, int destinationNode) {
        throw new IllegalStateException();
    }

    @Override
    public String getGraphTypeName() {
        return "StaticGraph";
    }
}
