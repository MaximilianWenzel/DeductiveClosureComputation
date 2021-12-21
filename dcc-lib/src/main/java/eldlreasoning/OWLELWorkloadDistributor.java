package eldlreasoning;

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

public class OWLELWorkloadDistributor extends WorkloadDistributor<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> {

    public OWLELWorkloadDistributor(List<? extends WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>>> workerModels) {
        super(workerModels);
    }

    @Override
    public List<Long> getRelevantWorkerIDsForAxiom(ELConceptInclusion axiom) {
        List<Long> workerIDs = new ArrayList<>();
        for (WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> worker : workerModels) {
            if (isRelevantAxiomToWorker(worker, axiom)) {
                workerIDs.add(worker.getID());
            }
        }
        return workerIDs;
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
