package eldlreasoning.models;

import java.util.Objects;
import java.util.stream.Stream;

public class IdxConjunction extends IdxConcept {
    protected IdxConcept firstConcept;
    protected IdxConcept secondConcept;

    public IdxConjunction(IdxConcept firstConcept, IdxConcept secondConcept) {
        this.firstConcept = firstConcept;
        this.secondConcept = secondConcept;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdxConjunction that = (IdxConjunction) o;
        return firstConcept.equals(that.firstConcept) && secondConcept.equals(that.secondConcept);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstConcept, secondConcept);
    }

    public IdxConcept getFirstConcept() {
        return firstConcept;
    }

    public IdxConcept getSecondConcept() {
        return secondConcept;
    }


    @Override
    public Stream<IdxConcept> streamOfConcepts() {
        Stream<IdxConcept> stream = Stream.concat(this.firstConcept.streamOfConcepts(), this.secondConcept.streamOfConcepts());
        return Stream.concat(Stream.of(this), stream);
    }
}
