package reasoning.saturator.distributed;

import data.Closure;
import data.DefaultClosure;
import data.ParallelToDo;
import reasoning.saturator.PartitionModel;

import java.util.ArrayList;
import java.util.List;

public class PartitionFactoryImpl {

/*
    protected abstract List<PartitionModel> initializePartitions();

    private void initVariables() {


        // init partitions
        List<PartitionModel> partitionModels = initializePartitions();
        this.partitions = new ArrayList<>();
        partitionModels.forEach(p -> {
            this.partitions.add(generateSaturatorPartition(p));
        });
    }

    protected SaturationPartition generateSaturatorPartition(PartitionModel partition) {
        Closure partitionClosure = new DefaultClosure();
        ParallelToDo partitionToDo = new ParallelToDo();

        return new SaturationPartition<>(partition.getRules(),
                partitionClosure,
                partitionToDo
        );
        this.dataset.getInitialAxioms().forEachRemaining(toDo::add);
    }

 */
}
