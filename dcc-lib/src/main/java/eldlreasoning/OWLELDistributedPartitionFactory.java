package eldlreasoning;

import data.IndexedELOntology;
import networking.ServerData;
import reasoning.saturation.models.DistributedPartitionFactory;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.models.WorkerModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OWLELDistributedPartitionFactory implements DistributedPartitionFactory {

    private final OWLELPartitionFactory partitionFactory;
    private final List<ServerData> partitionServerData;

    public OWLELDistributedPartitionFactory(IndexedELOntology elOntology, List<ServerData> partitionServerData) {
        this.partitionServerData = partitionServerData;
        this.partitionFactory = new OWLELPartitionFactory(elOntology, partitionServerData.size());
    }

    @Override
    public List<DistributedWorkerModel> generateDistributedPartitions() {
        List<WorkerModel> workerModels = partitionFactory.generatePartitions();

        List<DistributedWorkerModel> distributedPartitionModels = new ArrayList<>();

        Iterator<ServerData> serverDataIt = partitionServerData.iterator();
        for (WorkerModel workerModel : workerModels) {
            DistributedWorkerModel distributedPartitionModel = new DistributedWorkerModel(
                    workerModel.getRules(),
                    workerModel.getWorkerTerms(),
                    serverDataIt.next());
            distributedPartitionModels.add(distributedPartitionModel);
        }

        return distributedPartitionModels;
    }

}
