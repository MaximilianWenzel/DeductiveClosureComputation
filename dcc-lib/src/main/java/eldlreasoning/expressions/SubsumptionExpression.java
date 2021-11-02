package eldlreasoning.expressions;

import eldlreasoning.models.IdxConcept;

import java.util.Objects;
import java.util.stream.Stream;

public class SubsumptionExpression implements Expression {
    private IdxConcept subConcept;
    private IdxConcept superConcept;

    public SubsumptionExpression(IdxConcept subConcept, IdxConcept superConcept) {
        this.subConcept = subConcept;
        this.superConcept = superConcept;
    }

    public IdxConcept getSubConcept() {
        return subConcept;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubsumptionExpression that = (SubsumptionExpression) o;
        return subConcept.equals(that.subConcept) && superConcept.equals(that.superConcept);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subConcept, superConcept);
    }

    public IdxConcept getSuperConcept() {
        return superConcept;
    }

    @Override
    public Stream<IdxConcept> streamOfConcepts() {
        return Stream.concat(subConcept.streamOfConcepts(), superConcept.streamOfConcepts());
    }

    @Override
    public String toString() {
        return this.subConcept + " ⊑ " + this.superConcept;
    }
}
