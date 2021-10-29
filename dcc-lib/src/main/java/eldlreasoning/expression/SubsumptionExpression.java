package eldlreasoning.expression;

import eldlreasoning.models.IdxConcept;

import java.util.Objects;
import java.util.stream.Stream;

public class SubsumptionExpression implements Expression {
    private IdxConcept firstConcept;
    private IdxConcept secondConcept;

    public SubsumptionExpression(IdxConcept firstConcept, IdxConcept secondConcept) {
        this.firstConcept = firstConcept;
        this.secondConcept = secondConcept;
    }

    public IdxConcept getFirstConcept() {
        return firstConcept;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubsumptionExpression that = (SubsumptionExpression) o;
        return firstConcept.equals(that.firstConcept) && secondConcept.equals(that.secondConcept);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstConcept, secondConcept);
    }

    public IdxConcept getSecondConcept() {
        return secondConcept;
    }

    @Override
    public Stream<IdxConcept> streamOfConcepts() {
        return Stream.concat(firstConcept.streamOfConcepts(), secondConcept.streamOfConcepts());
    }
}
