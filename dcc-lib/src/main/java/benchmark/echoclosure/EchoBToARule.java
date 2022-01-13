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
    public Stream<EchoAxiom> streamOfInferences(EchoAxiom axiom) {
        Stream.Builder<EchoAxiom> inferences = Stream.builder();
        if (axiom instanceof EchoAxiomB) {
            inferences.add(new EchoAxiomA(axiom.getX() - 1));
        }
        return inferences.build();
    }
}
