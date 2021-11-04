package eldlreasoning.rules;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptInclusion;

import java.util.Queue;
import java.util.Set;

/**
 * C ⊑ E ⇐ C ⊑ D : D ⊑ E ∈ O
 */
public class UnfoldSubsumptionRule extends OWLELRule {

    private Iterable<ELConceptInclusion> ontology;

    public UnfoldSubsumptionRule(Queue<ELConceptInclusion> toDo, Set<ELConceptInclusion> ontology) {
        super(toDo);
        this.ontology = ontology;
    }

    @Override
    public void apply(ELConceptInclusion axiom) {
        ELConcept c = axiom.getSubConcept();
        ELConcept d = axiom.getSuperConcept();

        for (ELConceptInclusion conceptIncl : ontology) {
            if (d.equals(conceptIncl.getSubConcept())) {
                toDo.add(new ELConceptInclusion(c, conceptIncl.getSuperConcept()));
            }
        }
    }
}
