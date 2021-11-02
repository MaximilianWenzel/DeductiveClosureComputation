package eldlreasoning.expressions;

import eldlreasoning.models.IdxConcept;

import java.util.stream.Stream;

public interface ConceptStreamBuilder {
    /**
     * Returns a stream of all atomic and compound concepts of the given concept.
     */
    Stream<IdxConcept> streamOfConcepts();
}
