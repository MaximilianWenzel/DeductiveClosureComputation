package reasoning.saturation.distributed.state.partitionstates;

import networking.messages.InitializePartitionMessage;
import reasoning.saturation.distributed.SaturationPartition;

public class PartitionStateInitializing extends PartitionState {
    public PartitionStateInitializing(SaturationPartition partition) {
        super(partition);
    }

    @Override
    public void visit(InitializePartitionMessage message) {
        this.communicationChannel.initializePartition(message);
        this.partition.switchState(new PartitionStateEstablishingConnections(partition));
    }

}
