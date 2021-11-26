package reasoning.saturation;

import data.Closure;
import data.DefaultClosure;
import data.SingleThreadToDo;
import data.ToDoQueue;
import reasoning.reasoner.IncrementalReasonerImpl;
import reasoning.rules.Rule;
import reasoning.rules.SingleThreadedSaturationInferenceProcessor;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class SingleThreadedSaturation implements Saturation {

    private final Closure closure = new DefaultClosure();
    private final ToDoQueue toDo = new SingleThreadToDo();
    private Collection<? extends Rule> rules;

    private IncrementalReasonerImpl incrementalReasoner;

    public SingleThreadedSaturation() {
        this.rules = Collections.emptyList();
    }

    public SingleThreadedSaturation(Iterator<?> initialAxioms, Collection<? extends Rule> rules) {
        this.rules = rules;
        initializeRules();
        initializeToDoQueue(initialAxioms);

        this.incrementalReasoner = new IncrementalReasonerImpl(this.rules, this.closure);
    }

    private void initializeRules() {
        this.rules.forEach(r -> {
            r.setClosure(this.closure);
            r.setInferenceProcessor(new SingleThreadedSaturationInferenceProcessor(this.toDo));
        });
    }

    private void initializeToDoQueue(Iterator<?> initialAxioms) {
        initialAxioms.forEachRemaining(toDo::add);
    }

    public Closure saturate() {
        while (!toDo.isEmpty()) {
            incrementalReasoner.processAxiom(toDo.remove());
        }
        return closure;
    }

    @Override
    public void setRules(Collection<? extends Rule> rules) {
        this.rules = rules;
    }

    public Closure getClosure() {
        return closure;
    }
}
