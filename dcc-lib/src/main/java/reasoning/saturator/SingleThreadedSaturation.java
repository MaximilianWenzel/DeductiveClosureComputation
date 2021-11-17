package reasoning.saturator;

import data.*;
import reasoning.rules.Rule;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SingleThreadedSaturation<P, T> {

    private Closure<P> closure = new DefaultClosure<>();
    private ToDoQueue<P> toDo = new SingleThreadToDo<>();
    private Dataset<P, T> dataset;
    private Collection<? extends Rule<P>> rules;
    private final AtomicBoolean saturationFinished = new AtomicBoolean(true);

    public SingleThreadedSaturation(Dataset<P, T> dataset, Collection<? extends Rule<P>> rules) {
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
        saturationFinished.set(false);
        while (!toDo.isEmpty()) {
            process(toDo.remove());
        }
        saturationFinished.set(true);
        return closure;
    }

    public ToDoQueue<P> getToDo() {
        return toDo;
    }

    protected void process(P axiom) {
        if (closure.add(axiom)) {
            for (Rule<P> rule : rules) {
                rule.apply(axiom);
            }
        }
    }

    public Closure<P> getClosure() {
        return closure;
    }

    public boolean isSaturationFinished() {
        return this.saturationFinished.get();
    }
}
