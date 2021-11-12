package reasoning.saturator;

import data.*;
import reasoning.rules.Rule;

import java.util.*;

public class SingleThreadedSaturator<P, T> {

    private Closure<P> closure = new DefaultClosure<>();
    private ToDoQueue<P> toDo = new SingleThreadToDo<>();
    private Dataset<P, T> dataset;
    private Collection<? extends Rule<P>> rules;

    public SingleThreadedSaturator(Dataset<P, T> dataset, Collection<? extends Rule<P>> rules) {
        this.dataset = dataset;
        this.rules = rules;
        initializeRules();
        initializeToDoQueue();
    }

    private void initializeRules() {
        this.rules.forEach(r -> {
            r.setToDo(this.toDo);
            r.setClosure(this.closure);
        });
    }

    private void initializeToDoQueue() {
        dataset.getInitialAxioms().forEachRemaining(toDo::add);
    }

    public Set<P> saturate() {
        initializeRules();
        rules.forEach(r -> r.setToDo(this.toDo));
        initializeToDoQueue();

        while (!toDo.isEmpty()) {
            process(toDo.remove());
        }
        return closure;
    }

    protected void process(P axiom) {
        if (closure.add(axiom)) {
            for (Rule<P> rule : rules) {
                rule.apply(axiom);
            }
        }
    }
}
