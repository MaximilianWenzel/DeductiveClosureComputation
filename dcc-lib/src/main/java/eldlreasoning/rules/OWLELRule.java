package eldlreasoning.rules;

import eldlsyntax.ELConceptInclusion;
import reasoning.rules.Rule;

public abstract class OWLELRule extends Rule {

    public OWLELRule() {
        super();
    }

    @Override
    public void apply(Object axiom) {
        if (axiom instanceof ELConceptInclusion) {
            apply((ELConceptInclusion) axiom);
        }
    }

    public abstract void apply(ELConceptInclusion axiom);
}
