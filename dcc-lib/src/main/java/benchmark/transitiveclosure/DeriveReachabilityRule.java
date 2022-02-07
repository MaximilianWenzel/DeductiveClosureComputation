package benchmark.transitiveclosure;

import com.google.common.base.Stopwatch;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import reasoning.rules.Rule;

import java.util.stream.Stream;

/**
 * derived(x, z) :- derived(x, y), told(y, z)
 */
public class DeriveReachabilityRule extends Rule<ReachabilityClosure, Reachability> {

    int ruleDelayInNanoSec = 0;

    protected DeriveReachabilityRule() {

    }


    public DeriveReachabilityRule(int ruleDelayInNanoSec) {
        this.ruleDelayInNanoSec = ruleDelayInNanoSec;
    }

    @Override
    public Stream<Reachability> streamOfInferences(Reachability axiom) {
        Stream<Reachability> inferences;
        if (ruleDelayInNanoSec > 0) {
            Stopwatch s = Stopwatch.createStarted();
            while (true) {
                if (s.elapsed().toNanos() > ruleDelayInNanoSec) {
                    break;
                }
            }
        }

        if (axiom instanceof ToldReachability) {
            // y: source node
            // z: destination node
            // given: told(y, z)
            // search for: derived(x, y)
            int z = axiom.getDestinationNode();
            RoaringBitmap nodesWithConnectionToY = this.closure.getDerivedIncomingConnectedNodes(axiom.getSourceNode());
            inferences = nodesWithConnectionToY.stream().mapToObj(x -> new DerivedReachability(x, z));
        } else if (axiom instanceof DerivedReachability) {
            // x: source node
            // y: destination node
            // given: derived(x, y)
            // search for: told(y, z)
            RoaringBitmap reachableFromY = this.closure.getToldOutgoingConnectedNodes(axiom.getDestinationNode());
            int x = axiom.getSourceNode();
            inferences = reachableFromY.stream().mapToObj(z -> new DerivedReachability(x, z));
        } else {
            inferences = Stream.empty();
        }
        return inferences;
    }
}
