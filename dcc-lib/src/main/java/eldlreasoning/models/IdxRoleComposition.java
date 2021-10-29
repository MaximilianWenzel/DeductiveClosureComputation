package eldlreasoning.models;

import java.util.Objects;

public class IdxRoleComposition extends IdxRole {
    private IdxRole firstRole;
    private IdxRole secondRole;

    public IdxRoleComposition(IdxRole firstRole, IdxRole secondRole) {
        this.firstRole = firstRole;
        this.secondRole = secondRole;
    }

    public IdxRole getFirstRole() {
        return firstRole;
    }

    public IdxRole getSecondRole() {
        return secondRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdxRoleComposition that = (IdxRoleComposition) o;
        return firstRole.equals(that.firstRole) && secondRole.equals(that.secondRole);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstRole, secondRole);
    }
}
