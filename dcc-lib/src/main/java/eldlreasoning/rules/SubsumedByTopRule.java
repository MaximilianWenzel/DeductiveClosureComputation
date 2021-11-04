package eldlreasoning.rules;

import eldlsyntax.*;

import java.util.Queue;

/**
 * C ⊑ ⊤ ⇐ no preconditions
 */
public class SubsumedByTopRule extends OWLELRule {

    private final ELConcept topConcept;
    private final ConceptVisitor visitor = new ConceptVisitor();

    public SubsumedByTopRule(Queue<ELConceptInclusion> toDo, ELConcept topConcept) {
        super(toDo);
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
            toDo.add(new ELConceptInclusion(concept, topConcept));
        }

        @Override
        public void visit(ELConceptConjunction concept) {
            toDo.add(new ELConceptInclusion(concept, topConcept));
            concept.getFirstConjunct().accept(this);
            concept.getSecondConjunct().accept(this);
        }

        @Override
        public void visit(ELConceptExistentialRestriction concept) {
            toDo.add(new ELConceptInclusion(concept, topConcept));
            concept.getFiller().accept(this);
        }

        @Override
        public void visit(ELConceptName concept) {
            toDo.add(new ELConceptInclusion(concept, topConcept));
        }

        @Override
        public void visit(ELConceptTop concept) {
            toDo.add(new ELConceptInclusion(concept, topConcept));
        }
    }
}
