package benchmark.echoclosure;

import reasoning.rules.Rule;

import java.util.stream.Stream;

/**
 * b(x) :- a(x) .
 */
public class EchoAToBRule extends Rule<EchoClosure, EchoAxiom> {

    public EchoAToBRule() {

    }

    @Override
    public Stream<EchoAxiom> streamOfConclusions(EchoAxiom axiom) {
        Stream.Builder<EchoAxiom> conclusions = Stream.builder();
        if (axiom instanceof EchoAxiomA) {
            conclusions.add(new EchoAxiomB(axiom.getX() + 1));
        }
        return conclusions.build();
    }
}
