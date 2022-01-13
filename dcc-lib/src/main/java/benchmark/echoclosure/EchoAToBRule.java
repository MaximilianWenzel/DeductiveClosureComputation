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
    public Stream<EchoAxiom> streamOfInferences(EchoAxiom axiom) {
        Stream.Builder<EchoAxiom> inferences = Stream.builder();
        if (axiom instanceof EchoAxiomA) {
            inferences.add(new EchoAxiomB(axiom.getX() + 1));
        }
        return inferences.build();
    }
}
