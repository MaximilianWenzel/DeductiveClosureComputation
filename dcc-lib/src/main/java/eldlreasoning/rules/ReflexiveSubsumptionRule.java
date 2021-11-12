package eldlreasoning.rules;

import eldlsyntax.*;

/**
 * C ⊑ C ⇐ no premises
 */
public class ReflexiveSubsumptionRule extends OWLELRule {


    private final ConceptVisitor visitor = new ConceptVisitor();

    public ReflexiveSubsumptionRule() {
    }

    @Override
    public void apply(ELConceptInclusion axiom) {
        axiom.getSubConcept().accept(visitor);
        axiom.getSuperConcept().accept(visitor);
    }

    private class ConceptVisitor implements ELConcept.Visitor {

        @Override
        public void visit(ELConceptBottom concept) {
            addToToDo(new ELConceptInclusion(concept, concept));
        }

        @Override
        public void visit(ELConceptConjunction concept) {
            addToToDo(new ELConceptInclusion(concept, concept));
            concept.getFirstConjunct().accept(this);
            concept.getSecondConjunct().accept(this);
        }

        @Override
        public void visit(ELConceptExistentialRestriction concept) {
            addToToDo(new ELConceptInclusion(concept, concept));
            concept.getFiller().accept(this);
        }

        @Override
        public void visit(ELConceptName concept) {
            addToToDo(new ELConceptInclusion(concept, concept));
        }

        @Override
        public void visit(ELConceptTop concept) {
            addToToDo(new ELConceptInclusion(concept, concept));
        }
    }
}
