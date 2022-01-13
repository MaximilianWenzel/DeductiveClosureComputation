package benchmark.transitiveclosure;

import reasoning.rules.Rule;

import java.util.stream.Stream;

/**
 * derived(x, y) :- told(x, y)
 */
public class InitRule extends Rule<ReachabilityClosure, Reachability> {

    protected InitRule() {

    }

    @Override
    public Stream<Reachability> streamOfInferences(Reachability axiom) {
        Stream.Builder<Reachability> inferences = Stream.builder();

        if (axiom instanceof ToldReachability) {
            inferences.accept(new DerivedReachability(axiom.getSourceNode(), axiom.getDestinationNode()));
        }

        return inferences.build();
    }
}
