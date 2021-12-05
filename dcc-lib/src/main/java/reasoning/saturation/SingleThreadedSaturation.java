package reasoning.saturation;

import data.Closure;
import data.DefaultClosure;
import data.SingleThreadToDo;
import data.ToDoQueue;
import reasoning.reasoner.IncrementalReasonerImpl;
import reasoning.rules.Rule;
import reasoning.rules.SingleThreadedSaturationInferenceProcessor;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class SingleThreadedSaturation<C extends Closure<A>, A extends Serializable> implements Saturation<C, A> {

    private final ToDoQueue<A> toDo = new SingleThreadToDo<>();
    private C closure;
    private Collection<? extends Rule<C, A>> rules;

    private IncrementalReasonerImpl<C, A> incrementalReasoner;

    public SingleThreadedSaturation() {
        this.rules = Collections.emptyList();
    }

    public SingleThreadedSaturation(Iterator<A> initialAxioms, Collection<? extends Rule<C, A>> rules, C closure) {
        this.rules = rules;
        this.closure = closure;
        initializeRules();
        initializeToDoQueue(initialAxioms);

        this.incrementalReasoner = new IncrementalReasonerImpl<>(this.rules, this.closure);
    }

    private void initializeRules() {
        this.rules.forEach(r -> {
            r.setClosure(this.closure);
            r.setInferenceProcessor(new SingleThreadedSaturationInferenceProcessor(this.toDo));
        });
    }

    private void initializeToDoQueue(Iterator<A> initialAxioms) {
        initialAxioms.forEachRemaining(toDo::add);
    }

    public C saturate() {
        while (!toDo.isEmpty()) {
            incrementalReasoner.processAxiom(toDo.remove());
        }
        return closure;
    }

    public C getClosure() {
        return closure;
    }
}
