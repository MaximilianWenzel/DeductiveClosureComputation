package eldlreasoning.rules;

import eldlsyntax.ELConceptInclusion;

import java.util.Queue;

public abstract class OWLELRule implements Rule {
    protected Queue<ELConceptInclusion> toDo;

    public OWLELRule(Queue<ELConceptInclusion> toDo) {
        this.toDo = toDo;
    }
}
