package reasoning.saturation.distributed.states.partitionnode;

import networking.messages.InitializePartitionMessage;
import reasoning.saturation.distributed.SaturationPartition;

public class PartitionStateInitializing extends PartitionState {
    public PartitionStateInitializing(SaturationPartition partition) {
        super(partition);
    }

    @Override
    public void visit(InitializePartitionMessage message) {
        log.info("Partition initialization message received from control node. Initializing partition...");
        this.partition.initializePartition(message);
        log.info("Partition successfully initialized.");
        this.partition.switchState(new PartitionStateEstablishingConnections(partition));
    }

}
