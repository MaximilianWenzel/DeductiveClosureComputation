package benchmark;

import reasoning.rules.Rule;

/**
 * derived(x, y) :- told(x, y)
 */
public class InitRule extends Rule<ReachabilityClosure, Reachability> {
    @Override
    public void apply(Reachability axiom) {
        if (axiom instanceof ToldReachability) {
            processInference(new DerivedReachability(axiom.getSourceNode(), axiom.getDestinationNode()));
        }
    }
}
