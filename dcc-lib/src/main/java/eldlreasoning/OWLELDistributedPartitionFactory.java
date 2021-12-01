package eldlreasoning;

import data.IndexedELOntology;
import networking.ServerData;
import reasoning.saturation.models.DistributedPartitionFactory;
import reasoning.saturation.models.DistributedPartitionModel;
import reasoning.saturation.models.PartitionModel;

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
    public List<DistributedPartitionModel> generateDistributedPartitions() {
        List<PartitionModel> partitionModels = partitionFactory.generatePartitions();

        List<DistributedPartitionModel> distributedPartitionModels = new ArrayList<>();

        Iterator<ServerData> serverDataIt = partitionServerData.iterator();
        for (PartitionModel partitionModel : partitionModels) {
            DistributedPartitionModel distributedPartitionModel = new DistributedPartitionModel(
                    partitionModel.getRules(),
                    partitionModel.getPartitionTerms(),
                    serverDataIt.next());
            distributedPartitionModels.add(distributedPartitionModel);
        }

        return distributedPartitionModels;
    }

}
