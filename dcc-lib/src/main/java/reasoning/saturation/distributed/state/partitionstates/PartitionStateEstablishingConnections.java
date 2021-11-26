package reasoning.saturation.distributed.state.partitionstates;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import reasoning.saturation.distributed.SaturationPartition;

import java.util.concurrent.atomic.AtomicInteger;

public class PartitionStateEstablishingConnections extends PartitionState {
    private AtomicInteger establishedConnectionsToPartitions = new AtomicInteger(0);
    private int numberOfPartitions;


    public PartitionStateEstablishingConnections(SaturationPartition partition) {
        super(partition);
        this.numberOfPartitions = partition.getCommunicationChannel().getPartitions().size();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_SERVER_HELLO:
            case PARTITION_CLIENT_HELLO:
                establishedConnectionsToPartitions.getAndIncrement();
                if (establishedConnectionsToPartitions.get() == numberOfPartitions) {
                    partition.switchState(new PartitionStateInitialized(partition));
                    communicationChannel.sendToControlNode(SaturationStatusMessage.PARTITION_INFO_INITIALIZED);
                }
                break;
            default:
                throw new MessageProtocolViolationException();
        }
    }
}
