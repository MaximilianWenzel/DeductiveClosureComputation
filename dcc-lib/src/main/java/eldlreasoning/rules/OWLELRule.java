package eldlreasoning.rules;

import data.DefaultClosure;
import eldlsyntax.ELConceptInclusion;
import reasoning.rules.Rule;

public abstract class OWLELRule extends Rule<DefaultClosure<ELConceptInclusion>, ELConceptInclusion> {

    public OWLELRule() {
        super();
    }

    public abstract void apply(ELConceptInclusion axiom);
}
