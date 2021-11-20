package eldlreasoning.rules;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptInclusion;

import java.util.Set;

/**
 * derived(C ⊑ E) ⇐ derived(C ⊑ D) : told(D ⊑ E)
 */
public class UnfoldSubsumptionRule extends OWLELRule {

    private Iterable<ELConceptInclusion> ontology;

    public UnfoldSubsumptionRule(Set<ELConceptInclusion> ontology) {
        this.ontology = ontology;
    }

    @Override
    public void apply(ELConceptInclusion axiom) {
        ELConcept c = axiom.getSubConcept();
        ELConcept d = axiom.getSuperConcept();

        for (ELConceptInclusion conceptIncl : ontology) {
            if (d.equals(conceptIncl.getSubConcept())) {
                addToToDo(new ELConceptInclusion(c, conceptIncl.getSuperConcept()));
            }
        }
    }
}
