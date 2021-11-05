package eldlreasoning.saturator.parallel;

import eldlreasoning.rules.*;
import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptInclusion;
import eldlsyntax.IndexedELOntology;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class SaturatorPartition implements Runnable {

    private Set<ELConcept> conceptPartition;
    private Collection<Rule> rules;
    private IndexedELOntology ontologyFragment;
    private Set<ELConceptInclusion> closure;
    private BlockingQueue<ELConceptInclusion> toDo;
    private WorkloadManager workloadManager;

    public SaturatorPartition(Set<ELConcept> conceptPartition,
                              IndexedELOntology ontologyFragment,
                              Set<ELConceptInclusion> closure,
                              BlockingQueue<ELConceptInclusion> toDo,
                              WorkloadManager workloadManager) {
        this.conceptPartition = conceptPartition;
        this.ontologyFragment = ontologyFragment;
        this.closure = closure;
        this.toDo = toDo;
        this.workloadManager = workloadManager;
        init();
    }

    private void init() {
        // initialize all required rules
        rules = new UnifiedSet<>();
        rules.add(new ComposeConjunctionRule(toDo, ontologyFragment.getNegativeConcepts(), closure));
        rules.add(new DecomposeConjunctionRule(toDo));
        rules.add(new ReflexiveSubsumptionRule(toDo));
        rules.add(new SubsumedByTopRule(toDo, ontologyFragment.getTop()));
        rules.add(new UnfoldExistentialRule(toDo, closure));
        rules.add(new UnfoldSubsumptionRule(toDo, ontologyFragment.getOntologyAxioms()));
    }

    @Override
    public void run() {
        try {
            while (!workloadManager.isSaturationFinished()) {
                ELConceptInclusion axiom = toDo.take();
                process(axiom);
            }
        } catch (InterruptedException e) {
            // thread terminated
        }
    }

    private void process(ELConceptInclusion axiom) {
        if (WorkloadManager.checkIfOtherPartitionsRequireAxiom(conceptPartition, axiom)) {
            workloadManager.getToDo().add(axiom);
        }
        if (WorkloadManager.isRelevantAxiomToPartition(conceptPartition, axiom)) {
            if (closure.add(axiom)) {
                for (Rule rule : rules) {
                    rule.apply(axiom);
                }
            }
        }
    }

    public BlockingQueue<ELConceptInclusion> getToDo() {
        return toDo;
    }

    public Set<ELConcept> getConceptPartition() {
        return conceptPartition;
    }

    public Set<ELConceptInclusion> getClosure() {
        return closure;
    }
}
