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
    public Stream<Reachability> streamOfConclusions(Reachability axiom) {
        Stream.Builder<Reachability> conclusions = Stream.builder();

        if (axiom instanceof ToldReachability) {
            conclusions.accept(new DerivedReachability(axiom.getSourceNode(), axiom.getDestinationNode()));
        }

        return conclusions.build();
    }
}
