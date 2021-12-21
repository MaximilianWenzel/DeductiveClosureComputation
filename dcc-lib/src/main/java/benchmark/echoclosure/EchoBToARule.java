package benchmark.echoclosure;

import reasoning.rules.Rule;

/**
 * a(x) :- b(x) .
 */
public class EchoBToARule extends Rule<EchoClosure, EchoAxiom> {

    public EchoBToARule() {
    }

    @Override
    public void apply(EchoAxiom axiom) {
        if (axiom instanceof EchoAxiomB) {
            processInference(new EchoAxiomA(axiom.getX() - 1));
        }
    }
}
