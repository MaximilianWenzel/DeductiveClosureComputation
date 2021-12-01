package eldlreasoning.rules;

import eldlsyntax.*;

import java.io.Serializable;

/**
 * C ⊑ C ⇐ no premises
 */
public class ReflexiveSubsumptionRule extends OWLELRule {


    private final ConceptVisitor visitor = new ConceptVisitor();

    public ReflexiveSubsumptionRule() {
        super();
    }

    @Override
    public void apply(ELConceptInclusion axiom) {
        axiom.getSubConcept().accept(visitor);
        axiom.getSuperConcept().accept(visitor);
    }

    private class ConceptVisitor implements ELConcept.Visitor, Serializable {

        @Override
        public void visit(ELConceptBottom concept) {
            processInference(new ELConceptInclusion(concept, concept));
        }

        @Override
        public void visit(ELConceptConjunction concept) {
            processInference(new ELConceptInclusion(concept, concept));
            concept.getFirstConjunct().accept(this);
            concept.getSecondConjunct().accept(this);
        }

        @Override
        public void visit(ELConceptExistentialRestriction concept) {
            processInference(new ELConceptInclusion(concept, concept));
            concept.getFiller().accept(this);
        }

        @Override
        public void visit(ELConceptName concept) {
            processInference(new ELConceptInclusion(concept, concept));
        }

        @Override
        public void visit(ELConceptTop concept) {
            processInference(new ELConceptInclusion(concept, concept));
        }
    }
}
