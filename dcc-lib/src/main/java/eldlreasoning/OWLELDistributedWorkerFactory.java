package eldlreasoning;

import data.DefaultClosure;
import data.IndexedELOntology;
import eldlsyntax.ELConcept;
import eldlsyntax.ELConceptInclusion;
import networking.ServerData;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.models.DistributedWorkerFactory;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.models.WorkerModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OWLELDistributedWorkerFactory implements DistributedWorkerFactory<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> {

    private final OWLELWorkerFactory workerFactory;
    private final List<ServerData> workerServerData;

    public OWLELDistributedWorkerFactory(IndexedELOntology elOntology, List<ServerData> workerServerData) {
        this.workerServerData = workerServerData;
        this.workerFactory = new OWLELWorkerFactory(elOntology, workerServerData.size());
    }

    @Override
    public List<DistributedWorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>>> generateDistributedWorkers() {
        List<WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>>> workerModels = workerFactory.generateWorkers();

        List<DistributedWorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>>> distributedWorkerModels = new ArrayList<>();

        Iterator<ServerData> serverDataIt = workerServerData.iterator();
        for (WorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> workerModel : workerModels) {
            DistributedWorkerModel<DefaultClosure<ELConceptInclusion>, ELConceptInclusion, UnifiedSet<ELConcept>> distributedWorkerModel = new DistributedWorkerModel<>(
                    workerModel.getRules(),
                    workerModel.getWorkerTerms(),
                    serverDataIt.next());
            distributedWorkerModels.add(distributedWorkerModel);
        }

        return distributedWorkerModels;
    }

}
