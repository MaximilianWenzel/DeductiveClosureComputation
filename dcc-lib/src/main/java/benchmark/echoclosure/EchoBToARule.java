package benchmark.echoclosure;

import reasoning.rules.Rule;

import java.util.stream.Stream;

/**
 * a(x) :- b(x) .
 */
public class EchoBToARule extends Rule<EchoClosure, EchoAxiom> {

    public EchoBToARule() {
    }

    @Override
    public Stream<EchoAxiom> streamOfConclusions(EchoAxiom axiom) {
        Stream.Builder<EchoAxiom> conclusions = Stream.builder();
        if (axiom instanceof EchoAxiomB) {
            conclusions.add(new EchoAxiomA(axiom.getX() - 1));
        }
        return conclusions.build();
    }
}
