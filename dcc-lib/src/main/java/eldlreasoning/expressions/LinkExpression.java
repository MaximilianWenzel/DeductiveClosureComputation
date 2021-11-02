package eldlreasoning.expressions;

import eldlreasoning.models.IdxConcept;
import eldlreasoning.models.IdxRole;

import java.util.Objects;
import java.util.stream.Stream;

public class LinkExpression implements Expression {
    private IdxConcept firstConcept;
    private IdxRole role;
    private IdxConcept secondConcept;

    public LinkExpression(IdxConcept firstConcept, IdxRole role, IdxConcept secondConcept) {
        this.firstConcept = firstConcept;
        this.role = role;
        this.secondConcept = secondConcept;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkExpression that = (LinkExpression) o;
        return firstConcept.equals(that.firstConcept) && role.equals(that.role) && secondConcept.equals(that.secondConcept);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstConcept, role, secondConcept);
    }

    public IdxConcept getFirstConcept() {
        return firstConcept;
    }

    public IdxRole getRole() {
        return role;
    }

    public IdxConcept getSecondConcept() {
        return secondConcept;
    }

    @Override
    public Stream<IdxConcept> streamOfConcepts() {
        return Stream.concat(firstConcept.streamOfConcepts(), secondConcept.streamOfConcepts());
    }

    @Override
    public String toString() {
        return this.firstConcept + " -" + this.role + "-> " + this.secondConcept;
    }
}
