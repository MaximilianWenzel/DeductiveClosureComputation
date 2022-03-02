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

public class SingleThreadedSaturation<C extends Closure<A>, A extends Serializable> implements Saturation<C, A> {

    private final Queue<A> toDo = QueueFactory.getSingleThreadedToDo();
    private C closure;
    private Collection<? extends Rule<C, A>> rules;
    private Iterator<? extends A> initialAxioms;

    private DefaultIncrementalReasoner<C, A> incrementalReasoner;
    private SaturationConfiguration config = new SaturationConfiguration();
    private WorkerStatistics workerStats = new WorkerStatistics();
    private ControlNodeStatistics controlNodeStats = new ControlNodeStatistics();

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
            Collection<A> inferences = incrementalReasoner.getInferencesForGivenAxiom(toDo.remove());

            if (config.collectWorkerNodeStatistics()) {
                workerStats.startStopwatch(StatisticsComponent.WORKER_DISTRIBUTING_AXIOMS_TIME);
            }
            for (A inference : inferences) {
                workerStats.getNumberOfDerivedInferences().incrementAndGet();
                toDo.add(inference);
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
