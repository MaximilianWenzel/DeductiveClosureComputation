package benchmark.eldlreasoning;

import data.DefaultClosure;
import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptExistentialRestriction;
import eldlsyntax.ELConceptInclusion;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class OWLELWorkloadDistributor extends WorkloadDistributor<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> {

    private OWLELWorkloadDistributor() {

    }

    public OWLELWorkloadDistributor(List<? extends WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>>> workerModels) {
        super(workerModels);
    }

    @Override
    public Stream<Long> getRelevantWorkerIDsForAxiom(ELConceptInclusion axiom) {
        Stream.Builder<Long> workerIDs = Stream.builder();
        for (WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> worker : workerModels) {
            if (isRelevantAxiomToWorker(worker, axiom)) {
                workerIDs.add(worker.getID());
            }
        }
        return workerIDs.build();
    }

    public boolean isRelevantAxiomToWorker(WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> worker,
                                           ELConceptInclusion axiom) {
        Set<?> workerTerms = worker.getWorkerTerms();
        if (workerTerms.contains(axiom.getSubConcept())
                || workerTerms.contains(axiom.getSuperConcept())) {
            return true;
        } else if (axiom.getSuperConcept() instanceof ELConceptExistentialRestriction) {
            ELConceptExistentialRestriction exist = (ELConceptExistentialRestriction) axiom.getSuperConcept();
            if (workerTerms.contains(exist.getFiller())) {
                return true;
            }
        }
        return false;
    }

}
