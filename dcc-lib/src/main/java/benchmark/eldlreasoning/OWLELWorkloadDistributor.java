package benchmark.eldlreasoning;

import data.DefaultClosure;
import eldlsyntax.ELConceptExistentialRestriction;
import eldlsyntax.ELConceptInclusion;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.Set;
import java.util.stream.Stream;

public class OWLELWorkloadDistributor extends WorkloadDistributor<DefaultClosure<ELConceptInclusion>, ELConceptInclusion> {

    private int numberOfWorkers;

    private OWLELWorkloadDistributor() {

    }

    public OWLELWorkloadDistributor(int numberOfWorkers) {
        this.numberOfWorkers = numberOfWorkers;
    }

    @Override
    public Stream<Long> getRelevantWorkerIDsForAxiom(ELConceptInclusion axiom) {
        Set<Long> workerIDs = new UnifiedSet<>();
        workerIDs.add((long) (axiom.getSubConcept().hashCode() % numberOfWorkers));
        workerIDs.add((long) (axiom.getSuperConcept().hashCode() % numberOfWorkers));
        if (axiom.getSuperConcept() instanceof ELConceptExistentialRestriction) {
            ELConceptExistentialRestriction exist = (ELConceptExistentialRestriction) axiom.getSuperConcept();
            workerIDs.add((long) (exist.getFiller().hashCode() % numberOfWorkers));
        }
        return workerIDs.stream();
    }

}
