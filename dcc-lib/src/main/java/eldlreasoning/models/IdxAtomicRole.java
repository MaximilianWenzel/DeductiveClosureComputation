package eldlreasoning.models;

import java.util.Objects;

public class IdxAtomicRole extends IdxRole {
    private String id;

    public IdxAtomicRole(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdxAtomicRole that = (IdxAtomicRole) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
