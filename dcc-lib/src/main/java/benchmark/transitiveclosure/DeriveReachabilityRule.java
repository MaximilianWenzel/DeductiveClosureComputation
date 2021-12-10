package benchmark.transitiveclosure;

import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import reasoning.rules.Rule;

/**
 * derived(x, z) :- derived(x, y), told(y, z)
 */
public class DeriveReachabilityRule extends Rule<ReachabilityClosure, Reachability> {

    @Override
    public void apply(Reachability axiom) {
        if (axiom instanceof ToldReachability) {
            // y: source node
            // z: destination node
            // given: told(y, z)
            // search for: derived(x, y)
            int z = axiom.getDestinationNode();
            RoaringBitmap nodesWithConnectionToY = this.closure.getDerivedIncomingConnectedNodes(axiom.getSourceNode());
            nodesWithConnectionToY.forEach(new IntConsumer() {
                @Override
                public void accept(int x) {
                    processInference(new DerivedReachability(x, z));
                }
            });
        } else if (axiom instanceof DerivedReachability) {
            // x: source node
            // y: destination node
            // given: derived(x, y)
            // search for: told(y, z)
            RoaringBitmap reachableFromY = this.closure.getToldOutgoingConnectedNodes(axiom.getDestinationNode());
            reachableFromY.forEach(new IntConsumer() {
                @Override
                public void accept(int z) {
                    processInference(new DerivedReachability(axiom.getSourceNode(), z));
                }
            });
        }


    }
}
