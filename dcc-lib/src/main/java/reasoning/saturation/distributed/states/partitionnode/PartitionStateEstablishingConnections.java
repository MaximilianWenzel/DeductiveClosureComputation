package reasoning.saturation.distributed.states.partitionnode;

import enums.SaturationStatusMessage;
import exceptions.MessageProtocolViolationException;
import networking.messages.StateInfoMessage;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import reasoning.saturation.distributed.SaturationPartition;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class PartitionStateEstablishingConnections extends PartitionState {
    private Set<Long> establishedConnectionsToPartitions = new UnifiedSet<>();
    private int numberOfPartitions;


    public PartitionStateEstablishingConnections(SaturationPartition partition) {
        super(partition);
        log.info("Establishing connections to other partitions...");
        numberOfPartitions = partition.getCommunicationChannel().getPartitions().size();
        communicationChannel.connectToPartitionServers();
    }

    @Override
    public void visit(StateInfoMessage message) {
        switch (message.getStatusMessage()) {
            case PARTITION_SERVER_HELLO:
            case PARTITION_CLIENT_HELLO:
                establishedConnectionsToPartitions.add(message.getSenderID());
                if (establishedConnectionsToPartitions.size() == numberOfPartitions - 1) {
                    log.info("All connections to other partitions successfully initialized.");
                    partition.switchState(new PartitionStateInitialized(partition));
                    communicationChannel.sendToControlNode(SaturationStatusMessage.PARTITION_INFO_INITIALIZED);
                }
                break;
            default:
                messageProtocolViolation(message);
        }
    }
}
