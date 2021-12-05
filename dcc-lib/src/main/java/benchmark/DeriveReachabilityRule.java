package benchmark;

import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import reasoning.rules.Rule;

/**
 * derived(x, z) :- derived(x, y), derived(y, z)
 */
public class DeriveReachabilityRule extends Rule<ReachabilityClosure, Reachability> {

    @Override
    public void apply(Reachability axiom) {
        if (axiom instanceof ToldReachability) {
            return;
        }
        // x: source node
        // y: destination node
        // given: derived(x, y)
        // search for: derived(y, z)
        RoaringBitmap reachableFromY = this.closure.getDerivedOutgoingConnectedNodes(axiom.getDestinationNode());
        reachableFromY.forEach(new IntConsumer() {
            @Override
            public void accept(int z) {
                processInference(new DerivedReachability(axiom.getSourceNode(), z));
            }
        });

        // y: source node
        // z: destination node
        // given: derived(y, z)
        // search for: derived(x, y)
        int z = axiom.getDestinationNode();
        RoaringBitmap nodesWithConnectionToY = this.closure.getDerivedIncomingConnectedNodes(axiom.getSourceNode());
        nodesWithConnectionToY.forEach(new IntConsumer() {
            @Override
            public void accept(int x) {
                processInference(new DerivedReachability(x, z));
            }
        });
    }
}
