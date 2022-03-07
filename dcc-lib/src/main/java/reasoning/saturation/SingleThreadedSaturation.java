package reasoning.saturation;

import data.Closure;
import enums.StatisticsComponent;
import reasoning.reasoner.DefaultIncrementalReasoner;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import util.QueueFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

/**
 * A class which can be used in order to compute the deductive closure for a given set of axioms and a set of rules using only a single
 * thread.
 *
 * @param <C> Type of the resulting deductive closure.
 * @param <A> Type of the axioms in the deductive closure.
 */
public class SingleThreadedSaturation<C extends Closure<A>, A extends Serializable> implements Saturation<C, A> {

    private final Queue<A> toDo = QueueFactory.getSingleThreadedToDo();
    private final C closure;
    private final Collection<? extends Rule<C, A>> rules;
    private final Iterator<? extends A> initialAxioms;

    private final DefaultIncrementalReasoner<C, A> incrementalReasoner;
    private final WorkerStatistics workerStats = new WorkerStatistics();
    private final ControlNodeStatistics controlNodeStats = new ControlNodeStatistics();
    private SaturationConfiguration config = new SaturationConfiguration();

    public SingleThreadedSaturation(Iterator<? extends A> initialAxioms, Collection<? extends Rule<C, A>> rules,
                                    C closure) {
        this.rules = rules;
        this.closure = closure;
        this.initialAxioms = initialAxioms;
        initializeRules();

        this.incrementalReasoner = new DefaultIncrementalReasoner<>(this.rules, this.closure, config, workerStats);
    }

    public SingleThreadedSaturation(SaturationConfiguration config,
                                    Iterator<? extends A> initialAxioms, Collection<? extends Rule<C, A>> rules,
                                    C closure) {
        this.rules = rules;
        this.closure = closure;
        this.config = config;
        this.initialAxioms = initialAxioms;
        initializeRules();

        this.incrementalReasoner = new DefaultIncrementalReasoner<>(this.rules, this.closure, this.config, this.workerStats);
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
        controlNodeStats.startStopwatch(StatisticsComponent.CONTROL_NODE_SATURATION_TIME);
        initializeToDoQueue(initialAxioms);
        while (!toDo.isEmpty()) {
            Collection<A> conclusions = incrementalReasoner.getConclusionsForGivenAxiom(toDo.remove());

            if (config.collectWorkerNodeStatistics()) {
                workerStats.startStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
            }
            for (A conclusion : conclusions) {
                workerStats.getNumberOfDerivedConclusions().incrementAndGet();
                toDo.add(conclusion);
            }
            if (config.collectWorkerNodeStatistics()) {
                workerStats.stopStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
            }
        }
        controlNodeStats.stopStopwatch(StatisticsComponent.CONTROL_NODE_SATURATION_TIME);
        workerStats.getTodoIsEmptyEvent().incrementAndGet();
        workerStats.collectStopwatchTimes();
        controlNodeStats.collectStopwatchTimes();
        return closure;
    }

    public C getClosure() {
        return closure;
    }

    public ControlNodeStatistics getControlNodeStatistics() {
        return this.controlNodeStats;
    }

    public WorkerStatistics getWorkerStatistics() {
        return this.workerStats;
    }
}
