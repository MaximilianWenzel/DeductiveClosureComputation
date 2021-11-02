package eldlreasoning.models;

import java.util.Objects;
import java.util.stream.Stream;

public class IdxExistential extends IdxConcept {
    private IdxRole role;
    private IdxConcept filler;

    public IdxExistential(IdxRole role, IdxConcept filler) {
        this.role = role;
        this.filler = filler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdxExistential that = (IdxExistential) o;
        return role.equals(that.role) && filler.equals(that.filler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, filler);
    }

    @Override
    public Stream<IdxConcept> streamOfConcepts() {
        return Stream.concat(Stream.of(this), filler.streamOfConcepts());
    }

    public IdxRole getRole() {
        return role;
    }

    public IdxConcept getFiller() {
        return filler;
    }

    @Override
    public String toString() {
        return "∃" + this.role + "." + this.filler;
    }
}
