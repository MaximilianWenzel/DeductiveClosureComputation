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
import java.util.stream.Stream;

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

    public Collection<A> getInferencesForGivenAxiom(A axiom) {
        if (config.collectWorkerNodeStatistics()) {
            stats.startStopwatch(StatisticsComponent.WORKER_APPLYING_RULES_TIME_SATURATION);
        }
        try {
            if (closure.add(axiom)) {
                if (config.collectWorkerNodeStatistics()) {
                    this.stats.getNumberOfProcessedAxioms().incrementAndGet();
                }

                List<A> inferences = rules.stream().flatMap(rule -> rule.streamOfInferences(axiom))
                        .collect(Collectors.toList());
                return inferences;
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