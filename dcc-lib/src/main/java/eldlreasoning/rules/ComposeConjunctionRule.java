package eldlreasoning.rules;

import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptConjunction;
import eldlsyntax.ELConceptInclusion;

import java.util.Queue;
import java.util.Set;

/**
 * C ⊑ D1 ⊓ D2 ⇐ C ⊑ D1 ∧ C ⊑ D2 : D1 ⊓ D2 occurs negatively in ontology O.
 */
public class ComposeConjunctionRule extends OWLELRule {

    private final Set<ELConcept> negativeConceptsFromOntology;
    private final Set<ELConceptInclusion> closure;

    public ComposeConjunctionRule(Queue<ELConceptInclusion> toDo,
                                  Set<ELConcept> negativeConceptsFromOntology,
                                  Set<ELConceptInclusion> closure) {
        super(toDo);
        this.negativeConceptsFromOntology = negativeConceptsFromOntology;
        this.closure = closure;
    }

    @Override
    public void apply(ELConceptInclusion axiom) {

        ELConcept c = axiom.getSubConcept();
        ELConcept d1 = axiom.getSuperConcept();

        for (ELConceptInclusion conceptInclusion : closure) {
            if (c.equals(conceptInclusion.getSubConcept())) {
                ELConcept d2 = conceptInclusion.getSuperConcept();
                ELConceptConjunction conjunction = new ELConceptConjunction(d1, d2);
                if (this.negativeConceptsFromOntology.contains(conjunction)) {
                    this.toDo.add(new ELConceptInclusion(c, conjunction));
                }
                ELConceptConjunction conjunction2 = new ELConceptConjunction(d2, d1);
                if (this.negativeConceptsFromOntology.contains(conjunction2)) {
                    this.toDo.add(new ELConceptInclusion(c, conjunction2));
                }
            }
        }
    }
}
