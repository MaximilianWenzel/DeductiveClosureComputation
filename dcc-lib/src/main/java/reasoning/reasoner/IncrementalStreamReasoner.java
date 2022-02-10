package reasoning.reasoner;

import data.Closure;
import enums.StatisticsComponent;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.stream.Stream;

public class IncrementalStreamReasoner<C extends Closure<A>, A extends Serializable> {

    protected final Collection<? extends Rule<C, A>> rules;
    protected final Closure<A> closure;
    protected SaturationConfiguration config;
    protected WorkerStatistics stats;

    public IncrementalStreamReasoner(Collection<? extends Rule<C, A>> rules, Closure<A> closure,
                                     SaturationConfiguration config,
                                     WorkerStatistics stats) {
        this.rules = rules;
        this.closure = closure;
        this.config = config;
        this.stats = stats;
    }

    public Stream<A> getStreamOfInferencesForGivenAxioms(Stream<A> axioms) {

        if (config.collectWorkerNodeStatistics()) {
            stats.startStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
        }

        Stream<A> inferences = axioms.filter(closure::add)
                .flatMap(axiom -> rules.stream().flatMap(rule -> rule.streamOfInferences(axiom)));

        if (config.collectWorkerNodeStatistics()) {
            stats.stopStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
        }

        return inferences;
    }

    public Stream<A> getStreamOfInferencesForGivenAxiom(A axiom) {
        if (config.collectWorkerNodeStatistics()) {
            stats.startStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
        }
        try {
            if (closure.add(axiom)) {
                Stream<A> inferences = rules.stream().flatMap(rule -> rule.streamOfInferences(axiom));
                return inferences;
            } else {
                return Stream.empty();
            }
        } finally {
            if (config.collectWorkerNodeStatistics()) {
                stats.stopStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
            }
        }

    }
}