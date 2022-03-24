package reasoning.reasoner;

import data.Closure;
import enums.StatisticsComponent;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;

import java.io.Serializable;
import java.util.Collection;
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

    public Stream<A> getStreamOfConclusionsForGivenAxioms(Stream<A> axioms) {

        if (config.collectWorkerNodeStatistics()) {
            stats.startStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
        }

        Stream<A> conclusions = axioms.filter(closure::add)
                .flatMap(axiom -> rules.stream().flatMap(rule -> rule.streamOfConclusions(axiom)));

        if (config.collectWorkerNodeStatistics()) {
            stats.stopStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
        }

        return conclusions;
    }

    public Stream<A> getStreamOfConclusionsForGivenAxiom(A axiom) {
        if (config.collectWorkerNodeStatistics()) {
            stats.startStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
        }
        try {
            if (closure.add(axiom)) {
                if (config.collectWorkerNodeStatistics()) {
                    this.stats.getNumberOfProcessedAxioms().incrementAndGet();
                }

                Stream<A> conclusions = rules.stream().flatMap(rule -> rule.streamOfConclusions(axiom));
                return conclusions;
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