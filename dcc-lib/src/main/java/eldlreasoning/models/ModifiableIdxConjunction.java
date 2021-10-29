package eldlreasoning.models;

public class ModifiableIdxConjunction extends IdxConjunction {

    public ModifiableIdxConjunction(IdxConcept firstConcept, IdxConcept secondConcept) {
        super(firstConcept, secondConcept);
    }

    public void setFirstConcept(IdxConcept firstConcept) {
        this.firstConcept = firstConcept;
    }

    public void setSecondConcept(IdxConcept secondConcept) {
        this.secondConcept = secondConcept;
    }
}
