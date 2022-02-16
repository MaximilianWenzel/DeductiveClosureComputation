package reasoning.saturation;

import data.Closure;
import reasoning.reasoner.IncrementalStreamReasoner;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import util.QueueFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

public class SingleThreadedSaturation<C extends Closure<A>, A extends Serializable> implements Saturation<C, A> {

    private final Queue<A> toDo = QueueFactory.getSingleThreadedToDo();
    private C closure;
    private Collection<? extends Rule<C, A>> rules;

    private IncrementalStreamReasoner<C, A> incrementalReasoner;
    private SaturationConfiguration config = new SaturationConfiguration();
    private WorkerStatistics stats = new WorkerStatistics();

    public SingleThreadedSaturation(Iterator<? extends A> initialAxioms, Collection<? extends Rule<C, A>> rules,
                                    C closure) {
        this.rules = rules;
        this.closure = closure;
        initializeRules();
        initializeToDoQueue(initialAxioms);

        this.incrementalReasoner = new IncrementalStreamReasoner<>(this.rules, this.closure, config, stats);
    }

    public SingleThreadedSaturation(SaturationConfiguration config,
                                    Iterator<? extends A> initialAxioms, Collection<? extends Rule<C, A>> rules,
                                    C closure) {
        this.rules = rules;
        this.closure = closure;
        this.config = config;
        initializeRules();
        initializeToDoQueue(initialAxioms);

        this.incrementalReasoner = new IncrementalStreamReasoner<>(this.rules, this.closure, this.config, this.stats);
    }

    private void initializeRules() {
        this.rules.forEach(r -> {
            r.setClosure(this.closure);
        });
    }

    private void initializeToDoQueue(Iterator<? extends A> initialAxioms) {
        initialAxioms.forEachRemaining(toDo::add);
    }

    public C saturate() {
        while (!toDo.isEmpty()) {
            incrementalReasoner.getStreamOfInferencesForGivenAxiom(toDo.remove())
                    .forEach(toDo::add);
        }
        return closure;
    }

    public C getClosure() {
        return closure;
    }
}
