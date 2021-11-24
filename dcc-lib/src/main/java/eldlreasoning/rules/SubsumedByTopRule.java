package eldlreasoning.rules;

import eldlsyntax.*;

/**
 * C ⊑ ⊤ ⇐ no preconditions
 */
public class SubsumedByTopRule extends OWLELRule {

    private final ELConcept topConcept;
    private final ConceptVisitor visitor = new ConceptVisitor();

    public SubsumedByTopRule(ELConcept topConcept) {
        super();
        this.topConcept = topConcept;
    }

    @Override
    public void apply(ELConceptInclusion axiom) {
        axiom.getSubConcept().accept(visitor);
        axiom.getSuperConcept().accept(visitor);
    }

    private class ConceptVisitor implements ELConcept.Visitor {

        @Override
        public void visit(ELConceptBottom concept) {
            processInference(new ELConceptInclusion(concept, topConcept));
        }

        @Override
        public void visit(ELConceptConjunction concept) {
            processInference(new ELConceptInclusion(concept, topConcept));
            concept.getFirstConjunct().accept(this);
            concept.getSecondConjunct().accept(this);
        }

        @Override
        public void visit(ELConceptExistentialRestriction concept) {
            processInference(new ELConceptInclusion(concept, topConcept));
            concept.getFiller().accept(this);
        }

        @Override
        public void visit(ELConceptName concept) {
            processInference(new ELConceptInclusion(concept, topConcept));
        }

        @Override
        public void visit(ELConceptTop concept) {
            processInference(new ELConceptInclusion(concept, topConcept));
        }
    }
}
