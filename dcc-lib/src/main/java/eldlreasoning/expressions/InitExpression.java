package eldlreasoning.expressions;

import eldlreasoning.models.IdxConcept;

import java.util.Objects;
import java.util.stream.Stream;

public class InitExpression implements Expression {

    private final IdxConcept concept;

    public InitExpression(IdxConcept concept) {
        this.concept = concept;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InitExpression that = (InitExpression) o;
        return Objects.equals(concept, that.concept);
    }

    @Override
    public int hashCode() {
        return Objects.hash(concept);
    }

    public IdxConcept getConcept() {
        return concept;
    }

    @Override
    public Stream<IdxConcept> streamOfConcepts() {
        return Stream.of(concept);
    }

    @Override
    public String toString() {
        return  "Init(" + this.concept + ")";
    }
}
