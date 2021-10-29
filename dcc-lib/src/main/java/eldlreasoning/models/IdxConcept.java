package eldlreasoning.models;

import eldlreasoning.expression.ConceptStreamBuilder;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class IdxConcept implements ConceptStreamBuilder {
    protected final AtomicInteger negOccurs = new AtomicInteger(0);
    protected final AtomicInteger posOccurs = new AtomicInteger(0);

    public AtomicInteger getNegOccurs() {
        return negOccurs;
    }

    public AtomicInteger getPosOccurs() {
        return posOccurs;
    }
}
