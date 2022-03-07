package reasoning.reasoner;

import data.Closure;
import enums.StatisticsComponent;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class represents an implementation of a default incremental reasoner which deduces new axioms in the following way: If a given axiom
 * is not yet contained in the closure object, it is added to it, else the processing of the axiom ends and no new conclusions have been
 * derived. If it was not contained in the closure object yet, the incremental reasoner derives all conclusions for the given axiom using
 * the given rules and the closure object. The resulting set of conclusions is eventually returned.
 *
 * @param <C> Type of the closure object whose axioms are used to derive new conclusions.
 * @param <A> Type of the axioms in the closure object.
 */
public class DefaultIncrementalReasoner<C extends Closure<A>, A extends Serializable> {

    protected final Collection<? extends Rule<C, A>> rules;
    protected final Closure<A> closure;
    protected SaturationConfiguration config;
    protected WorkerStatistics stats;

    public DefaultIncrementalReasoner(Collection<? extends Rule<C, A>> rules, Closure<A> closure,
                                      SaturationConfiguration config,
                                      WorkerStatistics stats) {
        this.rules = rules;
        this.closure = closure;
        this.config = config;
        this.stats = stats;
    }

    public Collection<A> getConclusionsForGivenAxiom(A axiom) {
        if (config.collectWorkerNodeStatistics()) {
            stats.startStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
        }
        try {
            if (closure.add(axiom)) {
                if (config.collectWorkerNodeStatistics()) {
                    this.stats.getNumberOfProcessedAxioms().incrementAndGet();
                }

                List<A> conclusions = rules.stream().flatMap(rule -> rule.streamOfConclusions(axiom))
                        .collect(Collectors.toList());
                return conclusions;
            } else {
                return Collections.emptyList();
            }
        } finally {
            if (config.collectWorkerNodeStatistics()) {
                stats.stopStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
            }
        }

    }
}