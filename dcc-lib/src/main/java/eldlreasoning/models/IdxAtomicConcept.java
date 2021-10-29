package eldlreasoning.models;

import java.util.Objects;
import java.util.stream.Stream;

public class IdxAtomicConcept extends IdxConcept {
    private String id;

    public IdxAtomicConcept(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdxAtomicConcept that = (IdxAtomicConcept) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public Stream<IdxConcept> streamOfConcepts() {
        return Stream.of(this);
    }
}
