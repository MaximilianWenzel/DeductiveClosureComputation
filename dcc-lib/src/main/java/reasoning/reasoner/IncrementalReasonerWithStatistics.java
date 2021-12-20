package reasoning.reasoner;

import data.Closure;
import reasoning.rules.Rule;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;

import java.io.Serializable;
import java.util.Collection;

public class IncrementalReasonerWithStatistics<C extends Closure<A>, A extends Serializable> extends IncrementalReasonerImpl<C, A> {

    protected SaturationConfiguration config;
    protected WorkerStatistics stats;

    public IncrementalReasonerWithStatistics(Collection<? extends Rule<C, A>> rules, Closure<A> closure, SaturationConfiguration config, WorkerStatistics stats) {
        super(rules, closure);
        this.config = config;
        this.stats = stats;
    }

    @Override
    public void processAxiom(A axiom) {
        if (closure.add(axiom)) {
            if (config.collectStatistics()) {
                stats.getNumberOfProcessedAxioms().getAndIncrement();
            }
            for (Rule<C, A> rule : rules) {
                rule.apply(axiom);
            }
        }
    }
}
