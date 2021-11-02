package eldlreasoning.models;

import java.util.Objects;
import java.util.stream.Stream;

public class IdxConjunction extends IdxConcept {
    protected IdxConcept firstConjunct;
    protected IdxConcept secondConjunct;

    public IdxConjunction(IdxConcept firstConjunct, IdxConcept secondConjunct) {
        this.firstConjunct = firstConjunct;
        this.secondConjunct = secondConjunct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdxConjunction that = (IdxConjunction) o;
        return firstConjunct.equals(that.firstConjunct) && secondConjunct.equals(that.secondConjunct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstConjunct, secondConjunct);
    }

    public IdxConcept getFirstConjunct() {
        return firstConjunct;
    }

    public IdxConcept getSecondConjunct() {
        return secondConjunct;
    }


    @Override
    public Stream<IdxConcept> streamOfConcepts() {
        Stream<IdxConcept> stream = Stream.concat(this.firstConjunct.streamOfConcepts(), this.secondConjunct.streamOfConcepts());
        return Stream.concat(Stream.of(this), stream);
    }

    @Override
    public String toString() {
        return this.firstConjunct + " ⊓ " + this.secondConjunct;
    }
}
