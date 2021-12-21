package benchmark.echoclosure;

import reasoning.rules.Rule;

/**
 * b(x) :- a(x) .
 */
public class EchoAToBRule extends Rule<EchoClosure, EchoAxiom> {

    public EchoAToBRule() {

    }

    @Override
    public void apply(EchoAxiom axiom) {
        if (axiom instanceof EchoAxiomA) {
            processInference(new EchoAxiomB(axiom.getX() + 1));
        }
    }
}
