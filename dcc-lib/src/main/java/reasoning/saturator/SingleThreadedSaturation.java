package reasoning.saturator;

import data.*;
import reasoning.rules.Rule;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SingleThreadedSaturation {

    private final Closure closure = new DefaultClosure();
    private final ToDoQueue toDo = new SingleThreadToDo();
    private Collection<? extends Rule> rules;
    private final AtomicBoolean saturationFinished = new AtomicBoolean(true);

    public SingleThreadedSaturation(Iterator<?> initialAxioms, Collection<? extends Rule> rules) {
        this.rules = rules;
        initializeRules();
        initializeToDoQueue(initialAxioms);
    }

    private void initializeRules() {
        this.rules.forEach(r -> {
            r.setClosure(this.closure);
        });
    }

    private void initializeToDoQueue(Iterator<?> initialAxioms) {
        initialAxioms.forEachRemaining(toDo::add);
    }

    public Closure saturate() {
        saturationFinished.set(false);
        while (!toDo.isEmpty()) {
            process(toDo.remove());
        }
        saturationFinished.set(true);
        return closure;
    }

    public ToDoQueue getToDo() {
        return toDo;
    }

    protected void process(Object axiom) {
        if (closure.add(axiom)) {
            for (Rule rule : rules) {
                rule.apply(axiom);
            }
        }
    }

    public Closure getClosure() {
        return closure;
    }

    public boolean isSaturationFinished() {
        return this.saturationFinished.get();
    }
}
